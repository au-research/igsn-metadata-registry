package au.edu.ardc.registry.igsn.task;

import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.model.Attribute;
import au.edu.ardc.registry.exception.*;
import au.edu.ardc.registry.igsn.event.IGSNSyncedEvent;
import au.edu.ardc.registry.igsn.event.RequestExceptionEvent;
import au.edu.ardc.registry.igsn.model.IGSNTask;
import au.edu.ardc.registry.igsn.service.IGSNRegistrationService;
import au.edu.ardc.registry.igsn.service.IGSNRequestService;
import au.edu.ardc.registry.igsn.service.IGSNService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

public class SyncIGSNTask extends IGSNTask implements Runnable {

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
		super.setIdentifierValue(identifier.getValue());
		super.setRequestID(request.getId());
		this.igsnRegistrationService = igsnRegistrationService;
		this.applicationEventPublisher = applicationEventPublisher;
		this.igsnRequestService = igsnRequestService;
	}


	@Override
	public void run() {
		org.apache.logging.log4j.core.Logger requestLog = igsnRequestService.getLoggerFor(request);
		try {
			if(request.getAttribute(Attribute.START_TIME_REGISTER) == null){
				request.setAttribute(Attribute.START_TIME_REGISTER, new Date().getTime());
			}
			igsnRegistrationService.registerIdentifier(identifier.getValue(), request);
			request.setAttribute(Attribute.END_TIME_REGISTER, new Date().getTime());
			String tMsg = "Registered";
			if(request.getType().equals(IGSNService.EVENT_BULK_UPDATE) || request.getType().equals(IGSNService.EVENT_UPDATE)){
				tMsg = "Updated";
			}
			logger.info("{} MDS record for:{} request: {}", tMsg, identifier.getValue(), request.getId());
			request.incrementAttributeValue(Attribute.NUM_OF_IGSN_REGISTERED);
			// only update request message for bulk
			if(request.getType().equals(IGSNService.EVENT_BULK_UPDATE) || request.getType().equals(IGSNService.EVENT_BULK_MINT)){
				int totalCount = new Integer(request.getAttribute(Attribute.NUM_OF_RECORDS_RECEIVED));
				int numRegistered = new Integer(request.getAttribute(Attribute.NUM_OF_IGSN_REGISTERED));
				request.setMessage(String.format("%s MDS record %d out of %d", tMsg, numRegistered, totalCount));
			}

			requestLog.info(String.format("%s MDS record for: %s", tMsg, identifier.getValue()));
			logger.info("publishEvent (IGSNSyncedEvent) Identifier:{} request: {}", identifier.getValue(), request.getId());
			applicationEventPublisher.publishEvent(new IGSNSyncedEvent(identifier, request));

		}
		catch (IOException e) {
			// todo log the exception in the request log
			requestLog.error(e.getMessage());
			request.incrementAttributeValue(Attribute.NUM_OF_ERROR);
			request.incrementAttributeValue(Attribute.NUM_OF_FAILED_REGISTRATION);
			Thread.currentThread().interrupt();
			logger.error(e.getMessage());
			applicationEventPublisher.publishEvent(new RequestExceptionEvent(e.getMessage(), request));
		}
		catch (VersionContentAlreadyExistsException e) {
			requestLog.warn(e.getMessage());
			request.incrementAttributeValue(Attribute.NUM_OF_ERROR);
			request.incrementAttributeValue(Attribute.NUM_OF_FAILED_REGISTRATION);
			Thread.currentThread().interrupt();
			logger.warn(e.getMessage());
			applicationEventPublisher.publishEvent(new RequestExceptionEvent(e.getMessage(), request));
		}
		catch (VersionIsOlderThanCurrentException e) {
			requestLog.warn(e.getMessage());
			request.incrementAttributeValue(Attribute.NUM_OF_ERROR);
			request.incrementAttributeValue(Attribute.NUM_OF_FAILED_REGISTRATION);
			Thread.currentThread().interrupt();
			logger.warn(e.getMessage());
			applicationEventPublisher.publishEvent(new RequestExceptionEvent(e.getMessage(), request));
		}
		catch (ForbiddenOperationException e) {
			requestLog.error(e.getMessage());
			request.incrementAttributeValue(Attribute.NUM_OF_ERROR);
			request.incrementAttributeValue(Attribute.NUM_OF_FAILED_REGISTRATION);
			Thread.currentThread().interrupt();
			logger.warn(e.getMessage());
			applicationEventPublisher.publishEvent(new RequestExceptionEvent(e.getMessage(), request));
		}
		catch (RecordNotFoundException | NotFoundException e){
			requestLog.error(e.getMessage());
			request.incrementAttributeValue(Attribute.NUM_OF_ERROR);
			request.incrementAttributeValue(Attribute.NUM_OF_FAILED_REGISTRATION);
			Thread.currentThread().interrupt();
			logger.error(e.getClass() + e.getMessage());
			applicationEventPublisher.publishEvent(new RequestExceptionEvent(e.getMessage(), request));
		}
		catch (MDSClientException e){
			requestLog.error(e.getMessage());
			request.incrementAttributeValue(Attribute.NUM_OF_ERROR);
			request.incrementAttributeValue(Attribute.NUM_OF_FAILED_REGISTRATION);
			Thread.currentThread().interrupt();
			logger.error(e.getClass() + e.getMessage());
			applicationEventPublisher.publishEvent(new RequestExceptionEvent(e.getMessage(), request));
		}
		catch (Exception e) {
			requestLog.error(e.getMessage());
			request.incrementAttributeValue(Attribute.NUM_OF_ERROR);
			request.incrementAttributeValue(Attribute.NUM_OF_FAILED_REGISTRATION);
			Thread.currentThread().interrupt();
			logger.error(e.getClass() + e.getMessage());
			applicationEventPublisher.publishEvent(new RequestExceptionEvent(e.getMessage(), request));
		}
	}

	public Request getRequest() {
		return request;
	}

	public UUID getRequestID() {
		return request.getId();
	}

}
