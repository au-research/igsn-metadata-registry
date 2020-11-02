package au.edu.ardc.registry.igsn.task;

import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.event.RecordUpdatedEvent;
import au.edu.ardc.registry.exception.ForbiddenOperationException;
import au.edu.ardc.registry.igsn.entity.IGSNEventType;
import au.edu.ardc.registry.igsn.event.IGSNUpdatedEvent;
import au.edu.ardc.registry.igsn.service.IGSNRequestService;
import au.edu.ardc.registry.igsn.service.IGSNService;
import au.edu.ardc.registry.igsn.service.ImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import java.io.File;
import java.io.IOException;

public class ImportIGSNTask implements Runnable {

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
		this.importService = importService;
		this.applicationEventPublisher = applicationEventPublisher;
		this.igsnRequestService = igsnRequestService;
	}

	@Override
	public void run() throws ForbiddenOperationException {
		org.apache.logging.log4j.core.Logger requestLog = igsnRequestService.getLoggerFor(request);
		try {
			logger.info("Processing import file: {}", file.getAbsoluteFile());
			Identifier identifier = importService.importRequest(file, request);
			if (identifier != null) {
				applicationEventPublisher.publishEvent(new RecordUpdatedEvent(identifier.getRecord()));
				applicationEventPublisher.publishEvent(new IGSNUpdatedEvent(identifier, request));

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
		catch (IOException e) {
			// todo log the exception in the request log
			logger.warn(e.getMessage());
			requestLog.warn(e.getMessage());
		}
		catch (ForbiddenOperationException e) {
			if(request.getType().equals(IGSNService.EVENT_MINT))
			{
				request.setMessage(e.getMessage());
				requestLog.error(e.getMessage());
				throw new ForbiddenOperationException(e.getMessage());
			}
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
