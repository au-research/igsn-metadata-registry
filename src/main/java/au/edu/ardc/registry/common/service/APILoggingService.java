package au.edu.ardc.registry.common.service;

import au.edu.ardc.registry.common.config.MultiReadHttpServletRequest;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.message.StringMapMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;

import static au.edu.ardc.registry.common.util.Helpers.getClientIpAddress;

/**
 * A Service to enable logging specific for API requests and response
 */
@Service
public class APILoggingService {

	Logger logger = LoggerFactory.getLogger(APILoggingService.class);

	private org.apache.logging.log4j.Logger log = LogManager.getLogger(APILoggingService.class);

	/**
	 * Log the request using the built-in logger
	 * @param wrappedRequest MultiReadHttpServletRequest element
	 */
	public void logRequest(MultiReadHttpServletRequest wrappedRequest) {

		LinkedHashMap<String, Object> req = new LinkedHashMap<>();

		// include various single value fields
		req.put("type", "REQUEST");
		req.put("method", wrappedRequest.getMethod());
		req.put("auth", String.valueOf(wrappedRequest.getAuthType()));
		req.put("ip", getClientIpAddress(wrappedRequest));
		req.put("user_agent", wrappedRequest.getHeader("User-Agent"));
		req.put("url", String.valueOf(wrappedRequest.getRequestURL()));

		// include Headers
		List<String> excludedHeaders = new ArrayList<>();
		excludedHeaders.add("authorization");
		req.put("headers", getHeadersAsString(wrappedRequest, excludedHeaders));

		/*
		 * Include Body
		 *
		 * Does not work at the moment due to limitation in reading the ServletInputStream
		 * ServletInputStream can only be read once, Spring may reject the incoming
		 * request if the RequestBody is required Details
		 * https://www.jvt.me/posts/2020/05/25/read-servlet-request-body-multiple/
		 * https://stackoverflow.com/questions/10210645/http-servlet-request-lose-params-
		 * from-post-body-after-read-it-once https://stackoverflow.com/a/36619972/2257038
		 * and https://stackoverflow.com/a/30748533/2257038
		 */
		req.put("body", getBody(wrappedRequest));

		// logger.info(asJsonString(req));
		logger.info(req.toString());
	}

	/**
	 * Log the response using the built-in logger
	 * @param response HttpServletResponse
	 */
	public void logResponse(HttpServletResponse response) {
		LinkedHashMap<String, Object> res = new LinkedHashMap<>();

		res.put("type", "RESPONSE");
		res.put("status", response.getStatus());

		logger.info(res.toString());
	}

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
			return "";
		}
	}

	/**
	 * Log the request and response
	 * @param wrappedRequest the request
	 * @param servletResponse the response
	 */
	public void log(MultiReadHttpServletRequest wrappedRequest, HttpServletResponse servletResponse) {

		// due to Spring Security forward the error to the error Handler, the request URI
		// are lost in translation
		// can be reobtain with RequestDispatcher.FORWARD_REQUEST_URI if that exists
		String uri = wrappedRequest.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI) == null
				? wrappedRequest.getRequestURI()
				: (String) wrappedRequest.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);

		// every log should have a message
		String message = String.format("%s %s %s", wrappedRequest.getMethod(), uri, servletResponse.getStatus());

		// build out the StringMapMessage for structured API Event logging
		StringMapMessage msg = new StringMapMessage().with("message", message)
				.with("client.ip", getClientIpAddress(wrappedRequest))
				.with("client.address", getClientIpAddress(wrappedRequest))
				.with("user_agent.name", wrappedRequest.getHeader("User-Agent"))
				.with("user_agent.original", wrappedRequest.getHeader("User-Agent")).with("url.path", uri)
				.with("http.version", wrappedRequest.getProtocol())
				.with("http.request.method", wrappedRequest.getMethod())
				.with("http.version", wrappedRequest.getProtocol())
				.with("http.request.method", wrappedRequest.getMethod())
				.with("http.response.status_code", String.valueOf(servletResponse.getStatus()));

		// Referrer might not be always there
		String referrer = wrappedRequest.getHeader("referrer");
		if (referrer != null) {
			msg = msg.with("http.request.referrer", referrer);
		}

		// authType might not be always there
		String authType = wrappedRequest.getAuthType();
		if (authType != null) {
			msg = msg.with("url.auth", authType);
		}

		log.info(msg);
	}

}
