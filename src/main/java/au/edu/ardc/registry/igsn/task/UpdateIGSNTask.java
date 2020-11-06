package au.edu.ardc.registry.igsn.task;

import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.event.RecordUpdatedEvent;
import au.edu.ardc.registry.common.model.Attribute;
import au.edu.ardc.registry.exception.ForbiddenOperationException;
import au.edu.ardc.registry.exception.VersionContentAlreadyExistsException;
import au.edu.ardc.registry.exception.VersionIsOlderThanCurrentException;
import au.edu.ardc.registry.igsn.event.IGSNUpdatedEvent;
import au.edu.ardc.registry.igsn.event.RequestExceptionEvent;
import au.edu.ardc.registry.igsn.model.IGSNTask;
import au.edu.ardc.registry.igsn.service.IGSNRequestService;
import au.edu.ardc.registry.igsn.service.IGSNService;
import au.edu.ardc.registry.igsn.service.ImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class UpdateIGSNTask extends IGSNTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(UpdateIGSNTask.class);

	private final File file;

	private final Request request;

	private final ApplicationEventPublisher applicationEventPublisher;

	private final IGSNRequestService igsnRequestService;

	private final ImportService importService;

	private String identifierValue;

	private IGSNService igsnService;

	public UpdateIGSNTask(String identifierValue, File file, Request request, ImportService importService,
			ApplicationEventPublisher applicationEventPublisher, IGSNRequestService igsnRequestService) {
		this.identifierValue = identifierValue;
		super.setIdentifierValue(identifierValue);
		super.setRequestID(request.getId());
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
			request.incrementAttributeValue(Attribute.NUM_OF_RECORDS_UPDATED);
			requestLog.info(String.format("Updated Record with Identifier: %s", identifier.getValue()));
			applicationEventPublisher.publishEvent(new RecordUpdatedEvent(identifier.getRecord()));
			logger.info("Queue a sync task for identifier: {}", identifier.getValue());
			applicationEventPublisher.publishEvent(new IGSNUpdatedEvent(identifier, request));
		}
		catch (IOException e) {
			// todo log the exception in the request log
			requestLog.warn(e.getMessage());
			request.incrementAttributeValue(Attribute.NUM_OF_ERROR);
			Thread.currentThread().interrupt();
			logger.error(e.getMessage());
			applicationEventPublisher.publishEvent(new RequestExceptionEvent(e.getMessage(), request));
		}catch(ForbiddenOperationException e){
			requestLog.warn(e.getMessage());
			request.incrementAttributeValue(Attribute.NUM_OF_RECORD_FORBIDDEN);
			request.incrementAttributeValue(Attribute.NUM_OF_ERROR);
			Thread.currentThread().interrupt();
			logger.warn(e.getMessage());
			applicationEventPublisher.publishEvent(new RequestExceptionEvent(e.getMessage(), request));
		}catch(VersionIsOlderThanCurrentException e){
			requestLog.warn(e.getMessage());
			request.incrementAttributeValue(Attribute.NUM_OF_RECORD_CONTENT_NOT_CHANGED);
			request.incrementAttributeValue(Attribute.NUM_OF_ERROR);
			Thread.currentThread().interrupt();
			logger.warn(e.getMessage());
			applicationEventPublisher.publishEvent(new RequestExceptionEvent(e.getMessage(), request));
		}catch(VersionContentAlreadyExistsException e){
			requestLog.warn(e.getMessage());
			request.incrementAttributeValue(Attribute.NUM_OF_RECORD_CONTENT_NOT_CHANGED);
			request.incrementAttributeValue(Attribute.NUM_OF_ERROR);
			Thread.currentThread().interrupt();
			logger.warn(e.getMessage());
			applicationEventPublisher.publishEvent(new RequestExceptionEvent(e.getMessage(), request));
		}
	}

	public String getIdentifierValue() {
		return identifierValue;
	}

	public UUID getRequestID() {
		return request.getId();
	}

	public void setIdentifierValue(String identifierValue) {
		this.identifierValue = identifierValue;
	}

}
