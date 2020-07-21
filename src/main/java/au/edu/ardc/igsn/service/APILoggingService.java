package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.util.MultiReadHttpServletRequest;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;

import static au.edu.ardc.igsn.util.Helpers.getClientIpAddress;

/**
 * A Service to enable logging specific for API requests and response
 */
@Service
public class APILoggingService {

    Logger logger = LoggerFactory.getLogger(APILoggingService.class);

    /**
     * Log the request using the built-in logger
     *
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
         * ServletInputStream can only be read once,
         * Spring may reject the incoming request if the RequestBody is required
         * Details
         * https://www.jvt.me/posts/2020/05/25/read-servlet-request-body-multiple/
         * https://stackoverflow.com/questions/10210645/http-servlet-request-lose-params-from-post-body-after-read-it-once
         * https://stackoverflow.com/a/36619972/2257038 and https://stackoverflow.com/a/30748533/2257038
         */
        req.put("body", getBody(wrappedRequest));

        // logger.info(asJsonString(req));
        logger.info(req.toString());
    }

    /**
     * Log the response using the built-in logger
     *
     * @param response HttpServletResponse
     */
    public void logResponse(HttpServletResponse response) {
        LinkedHashMap<String, Object> res = new LinkedHashMap<>();

        res.put("type", "RESPONSE");
        res.put("status", response.getStatus());

        logger.info(res.toString());
    }

    /**
     * A helper method to display a collection of Headers provided in a request as a string format
     *
     * @param wrappedRequest  MultiReadHttpServletRequest
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
     * Attempt to return the body of the request
     * Does not work at the moment due to request input stream can only be read once
     *
     * @param wrappedRequest MultiReadHttpServletRequest
     * @return body
     */
    private String getBody(MultiReadHttpServletRequest wrappedRequest) {
        try {
            return IOUtils.toString(wrappedRequest.getInputStream(), wrappedRequest.getCharacterEncoding());
        } catch (Exception e) {
            return "";
        }
    }


}
