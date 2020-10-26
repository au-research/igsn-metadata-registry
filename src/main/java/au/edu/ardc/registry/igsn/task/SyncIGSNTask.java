package au.edu.ardc.registry.igsn.task;

import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.exception.ForbiddenOperationException;
import au.edu.ardc.registry.exception.VersionContentAlreadyExistsException;
import au.edu.ardc.registry.exception.VersionIsOlderThanCurrentException;
import au.edu.ardc.registry.igsn.service.IGSNRegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SyncIGSNTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(SyncIGSNTask.class);

	private final Identifier identifier;

	private final Request request;

	private final IGSNRegistrationService igsnRegistrationService;

	public SyncIGSNTask(Identifier identifier, Request request, IGSNRegistrationService igsnRegistrationService) {
		this.identifier = identifier;
		this.request = request;
		this.igsnRegistrationService = igsnRegistrationService;
	}

	@Override
	public void run() {
		try {
			igsnRegistrationService.registerIdentifier(identifier.getValue(), request);
			logger.info("Synced Identifier:{} request: {}", identifier.getValue(), request.getId());
			// todo dispatch an event to check if this is the last SyncIGSN task of the queue
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
			logger.error(e.getMessage());
		}
	}

}
