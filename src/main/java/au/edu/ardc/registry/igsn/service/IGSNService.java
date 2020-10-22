package au.edu.ardc.registry.igsn.service;

import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.model.*;
import au.edu.ardc.registry.common.provider.FragmentProvider;
import au.edu.ardc.registry.common.provider.IdentifierProvider;
import au.edu.ardc.registry.common.provider.Metadata;
import au.edu.ardc.registry.common.provider.MetadataProviderFactory;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.exception.VersionContentAlreadyExistsException;
import au.edu.ardc.registry.igsn.model.IGSNAllocation;
import au.edu.ardc.registry.igsn.model.IGSNTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class IGSNService {

	public static final String EVENT_MINT = "igsn.mint";

	public static final String EVENT_BULK_MINT = "igsn.bulk-mint";

	public static final String EVENT_UPDATE = "igsn.update";

	public static final String EVENT_BULK_UPDATE = "igsn.bulk-update";

	public static final String EVENT_RESERVE = "igsn.reserve";

	public static final String EVENT_TRANSFER = "igsn.transfer";

	private static final String IGSNallocationType = "urn:ardc:igsn:allocation";

	private static final Logger logger = LoggerFactory.getLogger(IGSNService.class);

	LinkedBlockingQueue<IGSNTask> syncQueue = new LinkedBlockingQueue<>();

	@Autowired
	SchemaService schemaService;

	@Autowired
	IGSNRequestService igsnRequestService;

	private Map<UUID, LinkedBlockingQueue<IGSNTask>> importQueue = new HashMap<>();

	@Autowired
	private ImportService importService;

	@PostConstruct
	public void init() {
		// intialsize
		importQueue = new HashMap<>();
		syncQueue = new LinkedBlockingQueue<>();

		// start a worker to work on the syncQueue
		new Thread(new IGSNTaskWorker(syncQueue, this)).start();
	}

	public Map<UUID, LinkedBlockingQueue<IGSNTask>> getImportQueue() {
		return importQueue;
	}

	public BlockingQueue<IGSNTask> getSyncQueue() {
		return syncQueue;
	}

	public BlockingQueue<IGSNTask> getImportQueueForAllocation(UUID allocationID) {
		if (importQueue.containsKey(allocationID)) {
			return importQueue.get(allocationID);
		}

		LinkedBlockingQueue<IGSNTask> queue = new LinkedBlockingQueue<>();
		importQueue.put(allocationID, queue);

		// start a new thread of an IGSNTaskWorker to follow this
		new Thread(new IGSNTaskWorker(queue, this)).start();

		return importQueue.get(allocationID);
	}

	/**
	 * Execute an IGSNTask. This method is executed manually by single operation (mint,
	 * update,..) as well as executed automatically by {@link IGSNTaskWorker#run()}.
	 * Exceptions should be caught here and logs properly
	 * @param task the {@link IGSNTask} to execute
	 */
	public void executeTask(IGSNTask task) {
		Request request = igsnRequestService.findById(String.valueOf(task.getRequestID()));
		org.apache.logging.log4j.core.Logger requestLogger = igsnRequestService.getLoggerFor(request);
		switch (task.getType()) {
		case IGSNTask.TASK_IMPORT:
			logger.info("Executing import task {}", task);
			try {
				Identifier identifier = importService.importRequest(task.getContentFile(),
						igsnRequestService.findById(String.valueOf(task.getRequestID())));
				if (identifier != null) {
					getSyncQueue().add(new IGSNTask(IGSNTask.TASK_SYNC, identifier.getValue(), task.getRequestID()));
				}
			}
			catch (IOException e) {
				// todo log the exception in the request log
				logger.error(e.getMessage());
			}
			logger.info("Finished import task {}", task);
			break;
		case IGSNTask.TASK_UPDATE:
			logger.debug("Executing update task {}", task);
			try {
				Identifier identifier = importService.updateRequest(task.getContentFile(), request);
				if (identifier != null) {
					logger.info("Queue a sync task for identifier: {}", identifier.getValue());
					getSyncQueue().add(new IGSNTask(IGSNTask.TASK_SYNC, identifier.getValue(), task.getRequestID()));
				}
			}
			catch (IOException e) {
				// todo log the exception in the request log
				logger.error(e.getMessage());
			}
			catch (VersionContentAlreadyExistsException e) {
				requestLogger.warn(e.getMessage());
				logger.warn(e.getMessage());
			}
			logger.debug("Finished update task {}", task);
			break;
		case IGSNTask.TASK_SYNC:
			logger.info("SYNC TASK {}", task);
			logger.info("Finish SYNC TASK {}", task);
			break;
		}
	}

	/**
	 * Check if a certain type of task is already in the queue. Uses
	 * {@link IGSNTask#equals(Object)} for comparison
	 * @param allocationID the {@link UUID} of the Allocation that we'll check in
	 * @param taskType the String taskType
	 * @param identifierValue the Identifier Value
	 * @return true if the same IGSNTask acting on the same Identifier is found
	 */
	public boolean hasIGSNTaskQueued(UUID allocationID, String taskType, String identifierValue) {
		IGSNTask task = new IGSNTask();
		task.setIdentifierValue(identifierValue);
		task.setType(taskType);

		if (taskType.equals(IGSNTask.TASK_SYNC)) {
			return getSyncQueue().contains(task);
		}

		return getImportQueueForAllocation(allocationID).contains(task);
	}

	/**
	 * Chunk and Queue the Request. Specifically used for bulk requests.
	 * @param request the {@link Request} to chunk and queue if necessary.
	 */
	@Async
	public void processMintOrUpdate(Request request) {

		// determine IGSNTask.type depends on the request.getType
		String taskType = IGSNTask.TASK_IMPORT;
		if (request.getType().equals(IGSNService.EVENT_UPDATE)
				|| request.getType().equals(IGSNService.EVENT_BULK_UPDATE)) {
			taskType = IGSNTask.TASK_UPDATE;
		}

		String payloadPath = request.getAttribute(Attribute.PAYLOAD_PATH);
		String dataPath = request.getAttribute(Attribute.DATA_PATH);
		String chunkedPayloadPath = dataPath + File.separator + "chunks";
		org.apache.logging.log4j.core.Logger requestLogger = igsnRequestService.getLoggerFor(request);

		// read the payload
		String payload = "";
		try {
			requestLogger.debug("Reading payload at {}", payloadPath);
			payload = Helpers.readFile(payloadPath);
		}
		catch (IOException e) {
			logger.error(e.getMessage());
		}

		// chunk the payload and queue an import task per payload
		try {
			UUID allocationID = UUID.fromString(request.getAttribute(Attribute.ALLOCATION_ID));
			Schema schema = schemaService.getSchemaForContent(payload);
			String fileExtension = Helpers.getFileExtensionForContent(payload);

			// create required provider
			FragmentProvider fragmentProvider = (FragmentProvider) MetadataProviderFactory.create(schema,
					Metadata.Fragment);
			IdentifierProvider identifierProvider = (IdentifierProvider) MetadataProviderFactory.create(schema,
					Metadata.Identifier);

			// todo check if chunkedPayloadPath is created properly
			Files.createDirectories(Paths.get(chunkedPayloadPath));
			requestLogger.debug("Created chunked directory at {}", chunkedPayloadPath);

			int count = fragmentProvider.getCount(payload);
			requestLogger.debug("Found {} fragments in payload", count);
			for (int i = 0; i < count; i++) {
				String content = fragmentProvider.get(payload, i);
				String outFilePath = chunkedPayloadPath + File.separator + i + fileExtension;
				Helpers.writeFile(outFilePath, content);
				requestLogger.debug("Written payload {} to {}", i, outFilePath);

				// queue the job
				IGSNTask task = new IGSNTask(taskType, new File(outFilePath), request.getId());
				task.setIdentifierValue(identifierProvider.get(content));

				getImportQueueForAllocation(allocationID).add(task);
				requestLogger.debug("Queued task {}", task);
				logger.info("Queued task {}", task);
			}

			request.setStatus(Request.Status.PROCESSED);
			igsnRequestService.save(request);
		}
		catch (IOException e) {
			logger.error(e.getMessage());
		}
	}

	/**
	 * Return the {@link IGSNAllocation}. The Allocation is extracted with first
	 * Identifier in the Content with {@link IdentifierProvider} and the User's Allocation
	 * @param content the XML String content to extract data from.
	 * @param user the {@link User} in the Request, with all of their Allocation and Scope
	 * @param scope the {@link Scope} to check with, usually Scope.Create or Scope.Update
	 * @return the {@link IGSNAllocation} that the User has the scoped access for
	 */
	public IGSNAllocation getIGSNAllocationForContent(String content, User user, Scope scope) {

		// obtain the first Identifier
		Schema schema = schemaService.getSchemaForContent(content);
		IdentifierProvider provider = (IdentifierProvider) MetadataProviderFactory.create(schema, Metadata.Identifier);
		List<String> identifiers = provider.getAll(content);
		String firstIdentifier = identifiers.get(0);

		// for each IGSN typed Allocation that the User has access to, find the Allocation
		// that has the prefix and namespace matches this first Identifier
		// todo refactor with Java8 Streaming API for performance
		List<Allocation> allocations = user.getAllocationsByType(IGSNallocationType);
		for (Allocation allocation : allocations) {
			IGSNAllocation ia = (IGSNAllocation) allocation;
			String prefix = ia.getPrefix();
			String namespace = ia.getNamespace();
			if (firstIdentifier.startsWith(prefix + "/" + namespace) && ia.getScopes().contains(scope)) {
				return ia;
			}
		}
		return null;
	}

}
