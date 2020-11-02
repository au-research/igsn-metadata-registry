package au.edu.ardc.registry.igsn.task;

import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.exception.ForbiddenOperationException;
import au.edu.ardc.registry.exception.NotFoundException;
import au.edu.ardc.registry.exception.VersionContentAlreadyExistsException;
import au.edu.ardc.registry.exception.VersionIsOlderThanCurrentException;
import au.edu.ardc.registry.igsn.event.IGSNSyncedEvent;
import au.edu.ardc.registry.igsn.service.IGSNRegistrationService;
import au.edu.ardc.registry.igsn.service.IGSNRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;

public class SyncIGSNTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(SyncIGSNTask.class);

	private final Identifier identifier;

	private final Request request;

	private final IGSNRegistrationService igsnRegistrationService;

	private final IGSNRequestService igsnRequestService;

	private final ApplicationEventPublisher applicationEventPublisher;

	public SyncIGSNTask(Identifier identifier, Request request, IGSNRegistrationService igsnRegistrationService,
			ApplicationEventPublisher applicationEventPublisher, IGSNRequestService igsnRequestService) {
		this.identifier = identifier;
		this.request = request;
		this.igsnRegistrationService = igsnRegistrationService;
		this.applicationEventPublisher = applicationEventPublisher;
		this.igsnRequestService = igsnRequestService;
	}

	@Override
	public void run() {
		org.apache.logging.log4j.core.Logger requestLog = igsnRequestService.getLoggerFor(request);
		try {
			igsnRegistrationService.registerIdentifier(identifier.getValue(), request);
			logger.info("Synced Identifier:{} request: {}", identifier.getValue(), request.getId());
			requestLog.info(String.format("Synced Identifier: %s", identifier.getValue()));
			applicationEventPublisher.publishEvent(new IGSNSyncedEvent(identifier, request));
			logger.info("publishEvent (IGSNSyncedEvent) Identifier:{} request: {}", identifier.getValue(), request.getId());
		}
		catch (IOException e) {
			// todo log the exception in the request log
			requestLog.warn(e.getMessage());
			logger.error(e.getMessage());
		}
		catch (VersionContentAlreadyExistsException e) {
			requestLog.warn(e.getMessage());
			logger.warn(e.getMessage());
		}
		catch (VersionIsOlderThanCurrentException e) {
			requestLog.warn(e.getMessage());
			logger.warn(e.getMessage());
		}
		catch (ForbiddenOperationException e) {
			requestLog.warn(e.getMessage());
			logger.warn(e.getMessage());
		}
		catch (NotFoundException e) {
			requestLog.warn(e.getMessage());
			logger.error(e.getMessage());
		}
		catch (Exception e){
			requestLog.warn(e.getMessage());
			logger.error(e.getClass() + e.getMessage());
		}
	}

	public Request getRequest() {
		return request;
	}

}