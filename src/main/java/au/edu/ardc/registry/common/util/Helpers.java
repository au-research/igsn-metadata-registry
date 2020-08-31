package au.edu.ardc.registry.common.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;
import org.springframework.core.io.ClassPathResource;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Helpers {

    private static final String[] HEADERS_TO_TRY = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"};

    /**
     * Get the Client true IP Address by trying different headers by priority
     *
     * @param request HttpServletRequest
     * @return ip       defaults to request.getRemoteAddr
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        for (String header : HEADERS_TO_TRY) {
            String ip = request.getHeader(header);
            if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    public static String readFile(String path) throws IOException {
        File file = new File(path);
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    }

    public static String readFileOnClassPath(String path) throws IOException {
        InputStream resource = new ClassPathResource(path).getInputStream();
        return IOUtils.toString(resource, StandardCharsets.UTF_8.name());
    }

    public static String probeContentType(String content) {
        Charset charset = StandardCharsets.UTF_8;
        byte[] byteArrray = content.getBytes(charset);
        return new Tika().detect(byteArrray);
    }

    public static String probeContentType(File file) throws IOException {
        return new Tika().detect(file);
    }
}
