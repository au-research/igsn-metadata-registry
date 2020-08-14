package au.edu.ardc.igsn.config;

import au.edu.ardc.igsn.service.APILoggingService;
import au.edu.ardc.igsn.util.MultiReadHttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

@Configuration
public class RequestLoggingFilter implements Filter {

    @Autowired
    APILoggingService loggingService;

    private final String[] excluded = {
            "actuator"
    };

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        MultiReadHttpServletRequest wrappedRequest =
                new MultiReadHttpServletRequest((HttpServletRequest) servletRequest);
        String path = wrappedRequest.getRequestURI();

        // exclude any path that matches the exclusion list
        boolean doLog = Arrays.stream(excluded).noneMatch(path::contains);

        // log the request with the wrappedRequest
        if (doLog) loggingService.logRequest(wrappedRequest);

        // do the next chain
        filterChain.doFilter(wrappedRequest, servletResponse);

        // log the response
        if (doLog) loggingService.logResponse((HttpServletResponse) servletResponse);
    }
}
