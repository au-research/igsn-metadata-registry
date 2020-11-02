package au.edu.ardc.registry.igsn.task;

import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.igsn.service.ImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransferIGSNTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(TransferIGSNTask.class);

	private final String identifierValue;

	private final Request request;

	private final ImportService importService;

	public TransferIGSNTask(String identifierValue, Request request, ImportService importService) {
		this.identifierValue = identifierValue;
		this.request = request;
		this.importService = importService;
	}

	@Override
	public void run() {
		try {
			importService.transferIdentifier(identifierValue, request);
			// todo handle returned identifier
		}
		catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

}
