package au.edu.ardc.registry.igsn.task;

import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.event.RecordUpdatedEvent;
import au.edu.ardc.registry.common.model.Attribute;
import au.edu.ardc.registry.exception.ContentNotSupportedException;
import au.edu.ardc.registry.exception.ForbiddenOperationException;
import au.edu.ardc.registry.igsn.entity.IGSNEventType;
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

public class ImportIGSNTask extends IGSNTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(ImportIGSNTask.class);

	private final File file;

	private final Request request;

	ApplicationEventPublisher applicationEventPublisher;

	ImportService importService;

	private final IGSNRequestService igsnRequestService;

	private String identifierValue;


	public ImportIGSNTask(String identifierValue, File file, Request request, ImportService importService,
			ApplicationEventPublisher applicationEventPublisher, IGSNRequestService  igsnRequestService) {
		this.identifierValue = identifierValue;
		this.file = file;
		this.request = request;
		super.setIdentifierValue(identifierValue);
		super.setRequestID(request.getId());
		this.importService = importService;
		this.applicationEventPublisher = applicationEventPublisher;
		this.igsnRequestService = igsnRequestService;
	}

	@Override
	public void run() {
		org.apache.logging.log4j.core.Logger requestLog = igsnRequestService.getLoggerFor(request);
		try {
			logger.info("Processing import file: {}", file.getAbsoluteFile());
			// only set it once
			if(request.getAttribute(Attribute.START_TIME_IMPORT) == null){
				request.setAttribute(Attribute.START_TIME_IMPORT, new Date().getTime());
			}

			Identifier identifier = importService.importRequest(file, request);
			request.incrementAttributeValue(Attribute.NUM_OF_RECORDS_CREATED);
			request.setAttribute(Attribute.END_TIME_IMPORT, new Date().getTime());
			if (identifier != null) {
				Record record = identifier.getRecord();
				int totalCount = new Integer(request.getAttribute(Attribute.NUM_OF_RECORDS_RECEIVED));
				int numCreated = new Integer(request.getAttribute(Attribute.NUM_OF_RECORDS_CREATED));
				request.setMessage(String.format("Imported %d out of %d", numCreated, totalCount));
				if(record != null){
					applicationEventPublisher.publishEvent(new RecordUpdatedEvent(identifier.getRecord()));
					applicationEventPublisher.publishEvent(new IGSNUpdatedEvent(identifier, request));
				}
			}
			if(request.getType().equals(IGSNService.EVENT_MINT)) {
				if(identifier != null){
					request.setMessage(String.format("Successfully created Identifier %s", identifier.getValue()));
				}else{
					request.setMessage("Error creating Identifier");
				}
			}
			logger.info("Processed import file: {}", file.getAbsoluteFile());
		}
		catch (IOException | ContentNotSupportedException e) {
			requestLog.warn(e.getMessage());
			logger.warn(e.getMessage());
			request.incrementAttributeValue(Attribute.NUM_OF_ERROR);
			if(request.getType().equals(IGSNService.EVENT_MINT)) {
				request.setMessage(e.getMessage());
			}
			else {
				applicationEventPublisher.publishEvent(new RequestExceptionEvent(e.getMessage(), request));
			}
		}
		catch (ForbiddenOperationException e) {
			requestLog.warn(e.getMessage());
			logger.warn(e.getMessage());
			request.incrementAttributeValue(Attribute.NUM_OF_RECORD_FORBIDDEN);
			request.incrementAttributeValue(Attribute.NUM_OF_ERROR);
			if(request.getType().equals(IGSNService.EVENT_MINT)) {
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
