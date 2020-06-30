package au.edu.ardc.igsn.config;

import au.edu.ardc.igsn.service.APILoggingService;
import au.edu.ardc.igsn.util.MultiReadHttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
public class RequestLoggingFilter implements Filter {

    @Autowired
    APILoggingService loggingService;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        MultiReadHttpServletRequest wrappedRequest =
                new MultiReadHttpServletRequest((HttpServletRequest) servletRequest);

        // log the request with the wrappedRequest
        loggingService.logRequest(wrappedRequest);

        // do the next chain
        filterChain.doFilter(wrappedRequest, servletResponse);

        // log the response
        loggingService.logResponse((HttpServletResponse) servletResponse);
    }
}
