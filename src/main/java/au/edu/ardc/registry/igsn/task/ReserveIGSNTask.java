package au.edu.ardc.registry.igsn.task;

import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.igsn.service.ImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReserveIGSNTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(ReserveIGSNTask.class);

	private String identifierValue;

	private Request request;

	private final ImportService importService;

	public ReserveIGSNTask(String identifierValue, Request request, ImportService importService) {
		this.identifierValue = identifierValue;
		this.request = request;
		this.importService = importService;
	}

	@Override
	public void run() {
		try {
			importService.reserveIGSNIdentifier(identifierValue, request);
			// todo notification? on reserved identifier
		}
		catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

}
