package au.edu.ardc.registry.common.config;

import au.edu.ardc.registry.common.service.APILoggingService;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

public class RequestLoggingFilter implements Filter {

	private final String[] excluded = { "actuator" };

	@Autowired
	APILoggingService loggingService;

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {

		MultiReadHttpServletRequest wrappedRequest = new MultiReadHttpServletRequest(
				(HttpServletRequest) servletRequest);
		String path = wrappedRequest.getRequestURI();

		// exclude any path that matches the exclusion list
		boolean doLog = Arrays.stream(excluded).noneMatch(path::contains);

		// log the request with the wrappedRequest
		if (doLog)
			loggingService.logRequest(wrappedRequest);

		// do the next chain
		filterChain.doFilter(wrappedRequest, servletResponse);

		// log the response
		loggingService.log(wrappedRequest, (HttpServletResponse) servletResponse);
		MDC.clear();
		// if (doLog)
		// loggingService.logResponse((HttpServletResponse) servletResponse);
	}

}
