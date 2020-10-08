package au.edu.ardc.registry.common.service;

import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.igsn.entity.IGSNEventType;
import au.edu.ardc.registry.common.entity.Request;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.springframework.stereotype.Service;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static au.edu.ardc.registry.common.util.Helpers.getClientIpAddress;

/**
 * A Service to enable logging specific for API requests and response
 */
@Service
public class APILoggingService {

	private final Logger log = LogManager.getLogger(APILoggingService.class);

	/**
	 * Log the request and response.
	 *
	 * Using ECS Specification for layout. Uses log4j2 as the Logger Implementation to log
	 * structured data
	 * @see <a href="https://www.elastic.co/guide/en/ecs/current/ecs-reference.html">ECS *
	 * specification</a>
	 * @param request the current request
	 * @param response the current response
	 */
	public void log(HttpServletRequest request, HttpServletResponse response) {
		ObjectNode ecs = getLoggableMessage(request, response);
		log.info(new ObjectMessage(ecs));
	}

	/**
	 * Return a ECS structure ready for logging
	 *
	 * @see <a href="https://www.elastic.co/guide/en/ecs/current/ecs-reference.html">ECS
	 * specification</a>
	 * @param request the current request
	 * @param response the current request
	 * @return ObjectNode representing the ECS jackson ObjectNode
	 */
	public ObjectNode getLoggableMessage(HttpServletRequest request, HttpServletResponse response) {
		ObjectMapper mapper = new ObjectMapper();

		ObjectNode ecs = mapper.createObjectNode();

		// client
		// client.address can contain comma separated list of IP
		// client.ip should contain the FIRST IP Address in that comma separated list
		ObjectNode client = mapper.createObjectNode();
		String clientAddress = getClientIpAddress(request);
		client.put("address", clientAddress);
		client.put("ip", clientAddress.split(",")[0].trim());

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
		String fullURL = request.getRequestURL().toString();

		// if this is a forwarded Error Request, we'll have to rebuild the full URL
		if (request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI) != null) {
			uri = (String) request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
			try {
				URL rebuiltURL = new URL(request.getRequestURL().toString());
				String host = rebuiltURL.getHost();
				String userInfo = rebuiltURL.getUserInfo();
				String scheme = rebuiltURL.getProtocol();
				int port = rebuiltURL.getPort();
				String query = (String) request.getAttribute(RequestDispatcher.FORWARD_QUERY_STRING);
				URI rebuiltURI = new URI(scheme, userInfo, host, port, uri, query, null);
				fullURL = rebuiltURI.toString();
			}
			catch (MalformedURLException | URISyntaxException ignored) {

			}
		}
		ObjectNode url = mapper.createObjectNode();
		url.put("path", uri);
		url.put("full", fullURL);

		// parse the full URL to fill out the other bits
		try {
			URL parsedURL = new URL(fullURL);
			url.put("scheme", parsedURL.getProtocol());
			url.put("port", parsedURL.getPort());
		}
		catch (MalformedURLException ignored) {

		}

		if (request.getQueryString() != null) {
			url.put("query", request.getQueryString());
		}

		ecs.set("url", url);

		// http
		ObjectNode http = mapper.createObjectNode();

		// http.request
		ObjectNode httpRequest = mapper.createObjectNode();
		httpRequest.put("method", request.getMethod());
		String referrer = request.getHeader("referrer");
		if (referrer != null) {
			httpRequest.put("referrer", referrer);
		}
		http.set("request", httpRequest);

		// http.response
		ObjectNode httpResponse = mapper.createObjectNode();
		httpResponse.put("status_code", String.valueOf(response.getStatus()));
		http.set("response", httpResponse);

		ecs.set("http", http);

		// useragent
		ObjectNode userAgent = mapper.createObjectNode();
		userAgent.put("original", request.getHeader("User-Agent"));
		ecs.set("user_agent", userAgent);

		// service
		ObjectNode service = mapper.createObjectNode();
		service.put("type", "igsn");
		service.put("name", "igsn-registry");
		service.put("kind", "event");
		ecs.set("service", service);

		// igsn
		Request igsn = (Request) request.getAttribute(String.valueOf(Request.class));
		if (igsn != null) {
			ecs.set("igsn", mapper.valueToTree(igsn));
		}

		// event
		ObjectNode event = mapper.createObjectNode();
		event.put("category", "web");
		event.put("action", determineEventAction(request));
		String outcome = (response.getStatus() < 200 || response.getStatus() > 299) ? "failure" : "success";
		event.put("outcome", outcome);
		ecs.set("event", event);

		// message
		ecs.put("message", String.format("%s %s %s", request.getMethod(), uri, response.getStatus()));

		return ecs;
	}

	/**
	 * Determine the ECS field event.action based on the current request
	 *
	 * By default, returns "api". If there's an {@link Request} in the request, the action
	 * will be in the associated {@link IGSNEventType}
	 * @see <a href=
	 * "https://www.elastic.co/guide/en/ecs/current/ecs-event.html">ecs-event</a>
	 * @param request the current {@link HttpServletRequest}
	 * @return the String representation of the event.action value
	 */
	private String determineEventAction(HttpServletRequest request) {

		// default action would be api
		String action = "api";

		// if it's an IGSN request, then the action is the type
		Request igsn = (Request) request.getAttribute(String.valueOf(Request.class));
		if (igsn != null) {
			action = igsn.getType();
		}

		return action;

	}

}
