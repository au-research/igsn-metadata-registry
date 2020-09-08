package au.edu.ardc.registry.common.service;

import au.edu.ardc.registry.common.config.MultiReadHttpServletRequest;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.igsn.entity.IGSNServiceRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.StringMapMessage;
import org.springframework.stereotype.Service;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.List;

import static au.edu.ardc.registry.common.util.Helpers.getClientIpAddress;

/**
 * A Service to enable logging specific for API requests and response
 */
@Service
public class APILoggingService {

	private final Logger log = LogManager.getLogger(APILoggingService.class);

	/**
	 * Log the request and response. Using ECS Speficiation and log4j2
	 * @see <a href="https://www.elastic.co/guide/en/ecs/current/ecs-reference.html">ECS
	 * specification</a>
	 * @param request the current request
	 * @param response the current response
	 */
	public void log(HttpServletRequest request, HttpServletResponse response) {
		ObjectMapper mapper = new ObjectMapper();

		ObjectNode ecs = mapper.createObjectNode();

		// client
		ObjectNode client = mapper.createObjectNode();
		client.put("ip", getClientIpAddress(request));

		// client.user
		User user = (User) request.getAttribute(String.valueOf(User.class));
		if (user != null) {
			ObjectNode userNode = mapper.createObjectNode();
			userNode.put("email", user.getEmail());
			userNode.put("id", user.getId().toString());
			userNode.put("name", user.getUsername());
			userNode.set("roles", mapper.valueToTree(user.getRoles()));
			client.set("user", userNode);
		}

		ecs.set("client", client);

		// url
		String uri = request.getRequestURI();
		if (request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI) != null) {
			uri = (String) request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
		}
		ObjectNode url = mapper.createObjectNode();
		url.put("path", uri);
		url.put("full", request.getRequestURL().toString());
		if (request.getQueryString() != null) {
			url.put("query", request.getQueryString());
		}
		if (request.getAuthType() != null) {
			url.put("auth", request.getAuthType());
		}
		ecs.set("url", url);

		// http
		ObjectNode http = mapper.createObjectNode();

		// http.request
		ObjectNode httpRequest = mapper.createObjectNode();
		httpRequest.put("method", request.getMethod());
		http.set("request", httpRequest);
		String referrer = request.getHeader("referrer");
		if (referrer != null) {
			httpRequest.put("referrer", referrer);
		}

		// http.response
		ObjectNode httpResponse = mapper.createObjectNode();
		httpResponse.put("status_code", String.valueOf(response.getStatus()));

		ecs.set("http", http);

		// useragent
		ObjectNode userAgent = mapper.createObjectNode();
		userAgent.put("original", request.getHeader("User-Agent"));
		ecs.set("user_agent", userAgent);

		// igsn
		IGSNServiceRequest igsn = (IGSNServiceRequest) request.getAttribute(String.valueOf(IGSNServiceRequest.class));
		if (igsn != null) {
			ecs.set("igsn", mapper.valueToTree(igsn));
		}

		// service
		ObjectNode service = mapper.createObjectNode();
		service.put("type", "igsn");
		service.put("name", "igsn-registry");
		service.put("kind", "event");
		service.put("event.category", "web");
		ecs.set("service", service);
		// todo event.action

		// message
		ecs.put("message", String.format("%s %s %s", request.getMethod(), uri, response.getStatus()));

		log.info(new ObjectMessage(ecs));
	}

}
