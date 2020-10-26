package au.edu.ardc.registry.igsn.task;

import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.event.RecordUpdatedEvent;
import au.edu.ardc.registry.exception.ForbiddenOperationException;
import au.edu.ardc.registry.exception.VersionContentAlreadyExistsException;
import au.edu.ardc.registry.exception.VersionIsOlderThanCurrentException;
import au.edu.ardc.registry.igsn.event.IGSNUpdatedEvent;
import au.edu.ardc.registry.igsn.service.ImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.io.File;
import java.io.IOException;

public class UpdateIGSNTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(UpdateIGSNTask.class);

	private File file;

	private Request request;

	private ApplicationEventPublisher applicationEventPublisher;

	private ImportService importService;

	private String identifierValue;

	public UpdateIGSNTask(String identifierValue, File file, Request request, ImportService importService,
			ApplicationEventPublisher applicationEventPublisher) {
		this.identifierValue = identifierValue;
		this.file = file;
		this.request = request;
		this.applicationEventPublisher = applicationEventPublisher;
		this.importService = importService;
	}

	@Override
	public void run() {
		try {
			Identifier identifier = importService.updateRequest(file, request);
			if (identifier != null) {
				applicationEventPublisher.publishEvent(new RecordUpdatedEvent(identifier.getRecord()));
				applicationEventPublisher.publishEvent(new IGSNUpdatedEvent(identifier, request));

				logger.info("Queue a sync task for identifier: {}", identifier.getValue());
			}
		}
		catch (IOException e) {
			// todo log the exception in the request log
			logger.error(e.getMessage());
		}
		catch (VersionContentAlreadyExistsException e) {
			logger.warn(e.getMessage());
		}
		catch (VersionIsOlderThanCurrentException e) {
			logger.warn(e.getMessage());
		}
		catch (ForbiddenOperationException e) {
			logger.warn(e.getMessage());
		}
		catch (Exception e) {
			logger.warn("GENERAL EXCEPTION:"  + e.getMessage());
		}
	}

	public String getIdentifierValue() {
		return identifierValue;
	}

	public void setIdentifierValue(String identifierValue) {
		this.identifierValue = identifierValue;
	}
}
