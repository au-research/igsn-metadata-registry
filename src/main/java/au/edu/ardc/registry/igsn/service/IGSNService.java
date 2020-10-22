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

	public void executeTask(IGSNTask task) throws InterruptedException {
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
			Thread.sleep(5000);
			logger.info("Finish SYNC TASK {}", task);
			break;
		}
	}

	/**
	 * Chunk and Queue the Request. Specifically used for bulk requests.
	 * @param request the {@link Request} to chunk and queue if necessary.
	 */
	@Async
	public void processMintOrUpdate(Request request) {

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

			FragmentProvider fragmentProvider = (FragmentProvider) MetadataProviderFactory.create(schema,
					Metadata.Fragment);
			Files.createDirectories(Paths.get(chunkedPayloadPath));
			// todo check if chunkedPayloadPath is created properly

			int count = fragmentProvider.getCount(payload);
			for (int i = 0; i < count; i++) {
				String content = fragmentProvider.get(payload, i);
				String outFilePath = chunkedPayloadPath + File.separator + i + fileExtension;
				Helpers.writeFile(outFilePath, content);

				// queue the job
				IGSNTask task = new IGSNTask(taskType, new File(outFilePath), request.getId());
				getImportQueueForAllocation(allocationID).add(task);
			}

			request.setStatus(Request.Status.PROCESSED);
			igsnRequestService.save(request);
		}
		catch (IOException e) {
			logger.error(e.getMessage());
		}
	}

	public IGSNAllocation getIGSNAllocationForContent(String content, User user, Scope scope) {
		Schema schema = schemaService.getSchemaForContent(content);
		IdentifierProvider provider = (IdentifierProvider) MetadataProviderFactory.create(schema, Metadata.Identifier);
		List<String> identifiers = provider.getAll(content);
		String firstIdentifier = identifiers.get(0);
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
