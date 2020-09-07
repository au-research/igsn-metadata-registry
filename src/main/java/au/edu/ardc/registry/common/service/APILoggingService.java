package au.edu.ardc.registry.common.service;

import au.edu.ardc.registry.common.config.MultiReadHttpServletRequest;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.igsn.entity.IGSNServiceRequest;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
	 * A helper method to display a collection of Headers provided in a request as a
	 * string format
	 * @param wrappedRequest MultiReadHttpServletRequest
	 * @param excludedHeaders The headers to not log (eg, authorization)
	 * @return headers
	 */
	private String getHeadersAsString(HttpServletRequest wrappedRequest, List<String> excludedHeaders) {
		StringBuilder headers = new StringBuilder();
		Enumeration<String> headerNames = wrappedRequest.getHeaderNames();
		if (headerNames != null) {
			while (headerNames.hasMoreElements()) {
				String headerName = headerNames.nextElement();
				String headerValue = wrappedRequest.getHeader(headerName);
				if (headerValue != null && !excludedHeaders.contains(headerName.toLowerCase())) {
					headers.append(headerName).append(": ").append(headerValue).append(" ");
				}
			}
		}
		return headers.toString();
	}

	/**
	 * Attempt to return the body of the request Does not work at the moment due to
	 * request input stream can only be read once
	 * @param wrappedRequest MultiReadHttpServletRequest
	 * @return body
	 */
	private String getBody(MultiReadHttpServletRequest wrappedRequest) {
		try {
			return IOUtils.toString(wrappedRequest.getInputStream(), wrappedRequest.getCharacterEncoding());
		}
		catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	/**
	 * Log the request and response
	 * @param request the request
	 * @param response the response
	 */
	public void log(HttpServletRequest request, HttpServletResponse response) {

		// due to Spring Security forward the error to the error Handler, the request URI
		// are lost in translation
		// can be reobtain with RequestDispatcher.FORWARD_REQUEST_URI if that exists
		String uri = request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI) == null ? request.getRequestURI()
				: (String) request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);

		// every log should have a message
		String message = String.format("%s %s %s", request.getMethod(), uri, response.getStatus());

		// build out the StringMapMessage for structured API Event logging
		// @formatter:off
		StringMapMessage msg = new StringMapMessage().with("message", message)
				.with("client.ip", getClientIpAddress(request))
				.with("user_agent.original", request.getHeader("User-Agent"))
				.with("url.path", uri);
		// @formatter:on

		// request
		msg = msg.with("http.request.method", request.getMethod());

		// response
		msg = msg.with("http.response.status_code", String.valueOf(response.getStatus()));

		// referrer
		String referrer = request.getHeader("referrer");
		if (referrer != null) {
			msg = msg.with("http.request.referrer", referrer);
		}
		// @formatter:on

		// infer User from the request (if set)
		User user = (User) request.getAttribute(String.valueOf(User.class));
		if (user != null) {
			// @formatter:off
			msg = msg.with("user.email", user.getEmail())
					.with("user.id", user.getId())
					.with("user.name", user.getUsername())
					.with("user.roles", user.getRoles());
			// @formatter:on
		}

		// infer igsn from the request (if set)
		// todo investigate option to extract this out into an IGSNLoggingService
		// so that each module can inject their own logging fragment
		IGSNServiceRequest igsn = (IGSNServiceRequest) request.getAttribute(String.valueOf(IGSNServiceRequest.class));
		if (igsn != null) {
			// @formatter:off
			msg = msg.with("igsn.id", igsn.getId())
					.with("igsn.path", igsn.getDataPath())
					.with("igsn.status", igsn.getStatus())
					.with("igsn.created", igsn.getCreatedAt())
					.with("igsn.updated", igsn.getUpdatedAt())
					.with("igsn.creator", igsn.getCreatedBy());

			// log IGSNRequestBody (if set)
			String IGSNRequestBody = (String) request.getAttribute("IGSNRequestBody");
			if (IGSNRequestBody != null) {
				msg = msg.with("http.request.body.content", IGSNRequestBody)
						.with("http.request.body.bytes", IGSNRequestBody.getBytes().length);
			}
			// @formatter:on
		}

		// authType might not be always there
		String authType = request.getAuthType();
		if (authType != null) {
			msg = msg.with("url.auth", authType);
		}

		log.info(msg);

	}

}
