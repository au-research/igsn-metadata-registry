package au.edu.ardc.registry.igsn.task;

import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.event.RecordUpdatedEvent;
import au.edu.ardc.registry.exception.ForbiddenOperationException;
import au.edu.ardc.registry.exception.VersionContentAlreadyExistsException;
import au.edu.ardc.registry.exception.VersionIsOlderThanCurrentException;
import au.edu.ardc.registry.igsn.event.IGSNUpdatedEvent;
import au.edu.ardc.registry.igsn.service.IGSNRequestService;
import au.edu.ardc.registry.igsn.service.ImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.io.File;
import java.io.IOException;

public class UpdateIGSNTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(UpdateIGSNTask.class);

	private final File file;

	private final Request request;

	private final ApplicationEventPublisher applicationEventPublisher;

	private final IGSNRequestService igsnRequestService;

	private final ImportService importService;

	private String identifierValue;

	public UpdateIGSNTask(String identifierValue, File file, Request request, ImportService importService,
			ApplicationEventPublisher applicationEventPublisher, IGSNRequestService igsnRequestService) {
		this.identifierValue = identifierValue;
		this.file = file;
		this.request = request;
		this.applicationEventPublisher = applicationEventPublisher;
		this.importService = importService;
		this.igsnRequestService = igsnRequestService;
	}

	@Override
	public void run() {
		org.apache.logging.log4j.core.Logger requestLog = igsnRequestService.getLoggerFor(request);
		try {
			Identifier identifier = importService.updateRequest(file, request);
			requestLog.info(String.format("Updated Record with Identifier: %s", identifier.getValue()));
			applicationEventPublisher.publishEvent(new RecordUpdatedEvent(identifier.getRecord()));
			applicationEventPublisher.publishEvent(new IGSNUpdatedEvent(identifier, request));
			logger.info("Queue a sync task for identifier: {}", identifier.getValue());
		}
		catch (IOException e) {
			// todo log the exception in the request log
			requestLog.warn(e.getMessage());
			logger.error(e.getMessage());
		} catch (Exception e) {
			requestLog.warn(e.getMessage());
			logger.warn(e.getMessage());
		}
	}

	public String getIdentifierValue() {
		return identifierValue;
	}

	public void setIdentifierValue(String identifierValue) {
		this.identifierValue = identifierValue;
	}

}
