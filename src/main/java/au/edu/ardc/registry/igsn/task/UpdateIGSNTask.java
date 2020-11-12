package au.edu.ardc.registry.igsn.task;

import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.event.RecordUpdatedEvent;
import au.edu.ardc.registry.common.model.Attribute;
import au.edu.ardc.registry.exception.ContentNotSupportedException;
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
import java.util.Date;
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
			// only set it once
			if(request.getAttribute(Attribute.START_TIME_UPDATE) == null){
				request.setAttribute(Attribute.START_TIME_UPDATE, new Date().getTime());
			}
			Identifier identifier = importService.updateRequest(file, request);
			request.incrementAttributeValue(Attribute.NUM_OF_RECORDS_UPDATED);
			request.setAttribute(Attribute.END_TIME_UPDATE, new Date().getTime());
			if (identifier != null) {
				Record record = identifier.getRecord();
				if(record != null){
					int totalCount = new Integer(request.getAttribute(Attribute.NUM_OF_RECORDS_RECEIVED));
					int numUpdated = new Integer(request.getAttribute(Attribute.NUM_OF_RECORDS_UPDATED));
					request.setMessage(String.format("Updated %d out of %d", numUpdated, totalCount));
					requestLog.info(String.format("Updated Record with Identifier: %s", identifier.getValue()));
					applicationEventPublisher.publishEvent(new RecordUpdatedEvent(identifier.getRecord()));
					applicationEventPublisher.publishEvent(new IGSNUpdatedEvent(identifier, request));
					logger.debug("Queued sync task for identifier: {}", identifier.getValue());
				}
			}
			if(request.getType().equals(IGSNService.EVENT_UPDATE)) {
				if(identifier != null){
					request.setMessage(String.format("Successfully updated Identifier: %s", identifier.getValue()));
				}else{
					request.setMessage(String.format("Error creating Identifier: %s", identifier.getValue()));
				}
			}
		}
		catch (IOException | ContentNotSupportedException e) {
			requestLog.warn(e.getMessage());
			logger.warn(e.getMessage());
			request.incrementAttributeValue(Attribute.NUM_OF_ERROR);
			if(request.getType().equals(IGSNService.EVENT_UPDATE)) {
				request.setMessage(e.getMessage());
			}
			else {
				applicationEventPublisher.publishEvent(new RequestExceptionEvent(e.getMessage(), request));
			}
		}catch(ForbiddenOperationException e){
			requestLog.warn(e.getMessage());
			logger.warn(e.getMessage());
			request.incrementAttributeValue(Attribute.NUM_OF_RECORD_FORBIDDEN);
			request.incrementAttributeValue(Attribute.NUM_OF_ERROR);
			if(request.getType().equals(IGSNService.EVENT_UPDATE)) {
				request.setMessage(e.getMessage());
			}
			else {
				applicationEventPublisher.publishEvent(new RequestExceptionEvent(e.getMessage(), request));
			}
		}catch(VersionIsOlderThanCurrentException | VersionContentAlreadyExistsException e){
			requestLog.warn(e.getMessage());
			logger.warn(e.getMessage());
			request.incrementAttributeValue(Attribute.NUM_OF_RECORD_CONTENT_NOT_CHANGED);
			request.incrementAttributeValue(Attribute.NUM_OF_ERROR);
			if(request.getType().equals(IGSNService.EVENT_UPDATE)) {
				request.setMessage(e.getMessage());
			}
			else {
				applicationEventPublisher.publishEvent(new RequestExceptionEvent(e.getMessage(), request));
			}
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
