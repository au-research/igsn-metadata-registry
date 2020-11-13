package au.edu.ardc.registry.igsn.task;

import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.model.Attribute;
import au.edu.ardc.registry.exception.ForbiddenOperationException;
import au.edu.ardc.registry.exception.NotChangedException;
import au.edu.ardc.registry.igsn.event.RequestExceptionEvent;
import au.edu.ardc.registry.igsn.event.TaskCompletedEvent;
import au.edu.ardc.registry.igsn.model.IGSNTask;
import au.edu.ardc.registry.igsn.service.IGSNRequestService;
import au.edu.ardc.registry.igsn.service.ImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Date;
import java.util.UUID;

public class TransferIGSNTask extends IGSNTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(ReserveIGSNTask.class);

	private final String identifierValue;

	private final Request request;

	private final ImportService importService;

	ApplicationEventPublisher applicationEventPublisher;

	private final IGSNRequestService igsnRequestService;

	public TransferIGSNTask(String identifierValue, Request request, ImportService importService,
						   ApplicationEventPublisher applicationEventPublisher, IGSNRequestService igsnRequestService) {
		this.identifierValue = identifierValue;
		super.setIdentifierValue(identifierValue);
		super.setRequestID(request.getId());
		this.request = request;
		this.importService = importService;
		this.applicationEventPublisher = applicationEventPublisher;
		this.igsnRequestService = igsnRequestService;
	}

	@Override
	public void run() {
		org.apache.logging.log4j.core.Logger requestLog = igsnRequestService.getLoggerFor(request);
		try {
			logger.info("Processing Reserving Identifier: {}", identifierValue);
			// only set it once
			if(request.getAttribute(Attribute.START_TIME_IMPORT) == null){
				request.setAttribute(Attribute.START_TIME_IMPORT, new Date().getTime());
			}
			Identifier identifier = importService.transferRequest(identifierValue, request);
			if (identifier != null) {
				int totalCount = new Integer(request.getAttribute(Attribute.NUM_OF_RECORDS_RECEIVED));
				int numCreated = new Integer(request.getAttribute(Attribute.NUM_OF_RECORDS_UPDATED));
				requestLog.info(String.format("Successfully Transferred Ownership of Record with Identifier: %s",
						identifier.getValue()));
				request.setMessage(String.format("Imported %d out of %d", numCreated, totalCount));
			}
			request.incrementAttributeValue(Attribute.NUM_OF_RECORDS_UPDATED);
			request.setAttribute(Attribute.END_TIME_IMPORT, new Date().getTime());

			String message = String.format("Processed Identifier: %s", identifierValue);
			logger.info(message);
			applicationEventPublisher.publishEvent(new TaskCompletedEvent(message, request));
		}
		catch (ForbiddenOperationException | NotChangedException e) {
			requestLog.error(e.getMessage());
			logger.warn(e.getMessage());
			request.incrementAttributeValue(Attribute.NUM_OF_ERROR);
			applicationEventPublisher.publishEvent(new RequestExceptionEvent(e.getMessage(), request));
		}

	}

	public UUID getRequestID() {
		return request.getId();
	}

}
