package au.edu.ardc.registry.igsn.service;

import au.edu.ardc.registry.igsn.config.IGSNProperties;
import au.edu.ardc.registry.igsn.entity.IGSNEventType;
import au.edu.ardc.registry.igsn.entity.IGSNServiceRequest;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.repository.IGSNServiceRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

@Service
public class IGSNService {

	Logger logger = LoggerFactory.getLogger(IGSNService.class);

	@Autowired
	IGSNProperties IGSNProperties;

	@Autowired
	private IGSNServiceRequestRepository repository;

	private Map<String, java.util.logging.Logger> loggers = new HashMap<>();

	public IGSNServiceRequest findById(String id) {
		Optional<IGSNServiceRequest> opt = repository.findById(UUID.fromString(id));
		return opt.orElse(null);
	}

	public IGSNServiceRequest save(IGSNServiceRequest request) {
		return repository.saveAndFlush(request);
	}

	public java.util.logging.Logger getLoggerFor(IGSNServiceRequest request) {
		String loggerID = "IGSNServiceRequest." + request.getId();

		if (loggers.containsKey(loggerID)) {
			return loggers.get(loggerID);
		}

		java.util.logging.Logger logger = java.util.logging.Logger.getLogger(loggerID);

		logger.setUseParentHandlers(false);
		try {
			FileHandler fileHandler = new FileHandler(request.getDataPath() + "/logs");
			logger.addHandler(fileHandler);
			fileHandler.setFormatter(new SimpleFormatter() {
				private static final String format = "[%1$tF %1$tT] [%2$-7s] %3$s %n";

				@Override
				public synchronized String format(LogRecord lr) {
					return String.format(format, new Date(lr.getMillis()), lr.getLevel().getLocalizedName(),
							lr.getMessage());
				}
			});
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		loggers.put(loggerID, logger);
		return logger;
	}

	public void closeLoggerFor(IGSNServiceRequest request) {
		String loggerID = "IGSNServiceRequest." + request.getId();

		if (!loggers.containsKey(loggerID)) {
			return;
		}

		java.util.logging.Logger logger = loggers.get(loggerID);
		for (Handler handle : logger.getHandlers()) {
			handle.close();
		}
		loggers.remove(loggerID);
	}

	public IGSNServiceRequest createRequest(User user, IGSNEventType type) {
		// create IGSNServiceRequest
		logger.debug("Creating IGSNServiceRequest for user: {}", user);
		IGSNServiceRequest request = new IGSNServiceRequest();
		request.setType(type);
		request.setCreatedAt(new Date());
		request.setCreatedBy(user.getId());
		request = repository.save(request);
		logger.debug("Created IGSNServiceRequest: id: {}", request.getId());

		// create request directory
		UUID id = request.getId();
		try {
			logger.debug("Creating data path");
			String separator = System.getProperty("file.separator");
			Path path = Paths.get(IGSNProperties.getDataPath() + separator + id.toString());
			logger.debug("Creating data path: {}", path.toAbsolutePath());
			Files.createDirectories(path);
			logger.debug("Created data path: {}", path.toAbsolutePath());
			request.setDataPath(path.toAbsolutePath().toString());
			// todo store the data path to the IGSNServiceRequest
		}
		catch (IOException e) {
			logger.error("Failed creating data path {}", e.getMessage());
		}

		request = repository.save(request);
		return request;
	}

}
