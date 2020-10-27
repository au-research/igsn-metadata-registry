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
import au.edu.ardc.registry.igsn.model.IGSNAllocation;
import au.edu.ardc.registry.igsn.model.IGSNTask;
import au.edu.ardc.registry.igsn.task.ImportIGSNTask;
import au.edu.ardc.registry.igsn.task.SyncIGSNTask;
import au.edu.ardc.registry.igsn.task.UpdateIGSNTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

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

	ThreadPoolExecutor syncIGSNExecutor;

	@Autowired
	SchemaService schemaService;

	@Autowired
	IGSNRequestService igsnRequestService;

	@Autowired
	ApplicationEventPublisher applicationEventPublisher;

	private Map<UUID, ThreadPoolExecutor> importExecutors = new HashMap<>();

	@Autowired
	private ImportService importService;

	@Autowired
	private IGSNRegistrationService igsnRegistrationService;

	@PostConstruct
	public void init() {
		importExecutors = new HashMap<>();
	}

	public void queueSync(Identifier identifier, Request request) {
		if (syncIGSNExecutor == null) {
			syncIGSNExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
		}

		syncIGSNExecutor.execute(new SyncIGSNTask(identifier, request, igsnRegistrationService, applicationEventPublisher));
	}

	public void queueImport(UUID allocationID, String identifierValue, File file, Request request) {
		if (!importExecutors.containsKey(allocationID)) {
			importExecutors.put(allocationID, (ThreadPoolExecutor) Executors.newFixedThreadPool(1));
		}

		importExecutors.get(allocationID)
				.execute(new ImportIGSNTask(identifierValue, file, request, importService, applicationEventPublisher));
	}

	public void queueUpdate(UUID allocationID, String identifierValue, File file, Request request) {
		if (!importExecutors.containsKey(allocationID)) {
			importExecutors.put(allocationID, (ThreadPoolExecutor) Executors.newFixedThreadPool(1));
		}

		importExecutors.get(allocationID)
				.execute(new UpdateIGSNTask(identifierValue, file, request, importService, applicationEventPublisher));
	}

	// todo check if there's any additional tasks in the request and init finalize if
	// there's none

	/**
	 * a request is considered finished if there are no tasks in the importQueue for that allocationID, and no more syncTask for that request
	 * @param request the {@link Request} in question
	 * @return true if the request is considered finished
	 */
	public boolean isRequestFinished(Request request) {
		//
		UUID allocationID = UUID.fromString(request.getAttribute(Attribute.ALLOCATION_ID));

		boolean hasTasksInImportQueue;
		boolean hasTasksInSyncQueue;

		// it has task in importqueue if there are still more tasks to do
		hasTasksInImportQueue = importExecutors.containsKey(allocationID) || importExecutors.get(allocationID).getTaskCount() > 0;

		// it has tasks in sync queue if there's any SyncIGSNTask with a request ID matching
		hasTasksInSyncQueue = syncIGSNExecutor.getQueue().stream().anyMatch(runnable -> {
			SyncIGSNTask task = (SyncIGSNTask) runnable;
			return task.getRequest().getId().equals(request.getId());
		});

		// request is finished when it doesn't have task in import queue or sync queue
		return !hasTasksInImportQueue && !hasTasksInSyncQueue;
	}

	public void checkRequest(Request request) {
		if (isRequestFinished(request)) {
			// finalize the request
			finalizeRequest(request);

			// shutdown the import Queue (& de-reference) to prevent importExecutors from running
			UUID allocationID = UUID.fromString(request.getAttribute(Attribute.ALLOCATION_ID));
			importExecutors.get(allocationID).shutdown();
			importExecutors.remove(allocationID);
		}
	}

	public void finalizeRequest(Request request) {
		request.setStatus(Request.Status.COMPLETED);
		igsnRequestService.save(request);
		igsnRequestService.closeLoggerFor(request);
	}

	public void shutdownSync() {
		syncIGSNExecutor.shutdown();
		syncIGSNExecutor = null;
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

		// early return if there's no queue setup
		if (!importExecutors.containsKey(allocationID)) {
			return false;
		}

		// early return if there's a queue but nothing in it
		if (importExecutors.get(allocationID).getTaskCount() == 0) {
			return false;
		}

		// check the queue content for import task with identical identifierValue
		BlockingQueue<Runnable> queue = importExecutors.get(allocationID).getQueue();
		return Arrays.stream(queue.toArray()).anyMatch(runnable -> {
			try {
				ImportIGSNTask task = (ImportIGSNTask) runnable;
				return task.getIdentifierValue().equals(identifierValue);
			}
			catch (Exception e) {
				logger.error(e.getMessage());
				return false;
			}
		});
	}

	/**
	 * Chunk and Queue the Request. Specifically used for bulk requests.
	 * @param request the {@link Request} to chunk and queue if necessary.
	 */
	@Async
	public void processMintOrUpdate(Request request) {

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
				String identifierValue = identifierProvider.get(content);
				String taskType = IGSNTask.TASK_IMPORT;
				if (request.getType().equals(IGSNService.EVENT_MINT)
						|| request.getType().equals(IGSNService.EVENT_BULK_MINT)) {
					queueImport(allocationID, identifierValue, new File(outFilePath), request);
				}
				else if (request.getType().equals(IGSNService.EVENT_UPDATE)
						|| request.getType().equals(IGSNService.EVENT_BULK_UPDATE)) {
					queueUpdate(allocationID, identifierValue, new File(outFilePath), request);
				}

				logger.info("Queued task {} for Identifier: {}", taskType, identifierValue);
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
