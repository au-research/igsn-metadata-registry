package au.edu.ardc.registry.igsn.service;

import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.model.Attribute;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.repository.RequestRepository;
import au.edu.ardc.registry.common.service.RequestService;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.igsn.entity.IGSNEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

@Service
public class IGSNRequestService {

	Logger logger = LoggerFactory.getLogger(IGSNRequestService.class);

	@Autowired
	RequestService requestService;

	@Autowired
	private RequestRepository repository;

	public Request findById(String id) {
		return requestService.findById(id);
	}

	public Request save(Request request) {
		return repository.saveAndFlush(request);
	}

	/**
	 * Creates an IGSN Request
	 * @param user the {@link User} that initiate the Request
	 * @param type the Type of the Request
	 * @param payload the Payload of the Request to store immediately
	 * @return the {@link Request} with type IGSN
	 * @throws IOException when creating and/or storing directory
	 */
	public Request createRequest(User user, String type, String payload) throws IOException {
		// create IGSNServiceRequest
		logger.debug("Creating IGSNServiceRequest for user: {}", user);
		Request request = new Request();
		request.setType(type);
		request.setCreatedAt(new Date());
		request.setUpdatedAt(new Date());
		request.setCreatedBy(user.getId());
		request.setStatus(Request.Status.CREATED);
		request = repository.save(request);
		logger.debug("Created IGSNServiceRequest: id: {}", request.getId());

		// create request directory
		try {
			logger.debug("Creating data path");
			Path path = Paths.get(requestService.getDataPathFor(request));
			logger.debug("Creating data path: {}", path.toAbsolutePath());
			Files.createDirectories(path);
			logger.debug("Created data path: {}", path.toAbsolutePath());
			request.setAttribute(Attribute.DATA_PATH, path.toAbsolutePath().toString());
		}
		catch (IOException e) {
			logger.error("Failed creating data path {}", e.getMessage());
		}

		// log path
		request.setAttribute(Attribute.LOG_PATH, requestService.getLoggerPathFor(request));

		// insert the payload
		if (payload != null) {
			String dataPath = requestService.getDataPathFor(request);
			String fileExtension = Helpers.getFileExtensionForContent(payload);
			String payLoadContentPath = dataPath + File.separator + "payload" + fileExtension;
			Helpers.writeFile(payLoadContentPath, payload);
			request.setAttribute(Attribute.PAYLOAD_PATH, payLoadContentPath);
		}

		request = repository.save(request);
		return request;
	}

	public org.apache.logging.log4j.core.Logger getLoggerFor(Request request) {
		return requestService.getLoggerFor(request);
	}

	public void closeLoggerFor(Request request) {
		requestService.closeLoggerFor(request);
	}

}
