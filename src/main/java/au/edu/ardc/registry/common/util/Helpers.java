package au.edu.ardc.registry.common.util;

import au.edu.ardc.registry.common.model.schema.JSONValidator;
import au.edu.ardc.registry.common.model.schema.XMLValidator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.springframework.core.io.ClassPathResource;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Helpers {

	private static final String[] HEADERS_TO_TRY = { "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP",
			"HTTP_X_FORWARDED_FOR", "HTTP_X_FORWARDED", "HTTP_X_CLUSTER_CLIENT_IP", "HTTP_CLIENT_IP",
			"HTTP_FORWARDED_FOR", "HTTP_FORWARDED", "HTTP_VIA", "REMOTE_ADDR" };

	/**
	 * Get the Client true IP Address by trying different headers by priority
	 * @param request HttpServletRequest
	 * @return ip defaults to request.getRemoteAddr
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

	public static void writeFile(String filePath, String content) throws IOException {
		File inputIGSNFile = new File(filePath);
		boolean newFile = inputIGSNFile.createNewFile();
		FileWriter writer = new FileWriter(filePath);
		writer.write(content);
		writer.close();
	}

	public static void appendToFile(String filePath, String content) throws IOException {
		File inputIGSNFile = new File(filePath);
		String newLine = System.getProperty("line.separator");
		boolean newFile = inputIGSNFile.createNewFile();
		FileWriter writer = new FileWriter(filePath, true);
		writer.write(content + newLine);
		writer.close();
	}

	public static void newOrEmptyDirecory(String dirPath) throws IOException {
		File directory = new File(dirPath);
		if (directory.exists() && directory.isDirectory()) {
			boolean deleted = directory.delete();
		}
		Files.createDirectory(Paths.get(dirPath));

	}

	/**
	 * @param content a String content of a file
	 * @return the file extension for the file eg .xml or .json if can not be found a
	 * ".bin" extension is 'assumed'
	 */
	public static String getFileExtensionForContent(String content) {
		try {
			String mediaType = Helpers.probeContentType(content);
			TikaConfig config = TikaConfig.getDefaultConfig();
			MimeType mimeType = config.getMimeRepository().forName(mediaType);
			return mimeType.getExtension();
		}
		catch (MimeTypeException e) {
			return ".bin";
		}
	}

	public static String readFileOnClassPath(String path) throws IOException {
		InputStream resource = new ClassPathResource(path).getInputStream();
		return IOUtils.toString(resource, StandardCharsets.UTF_8.name());
	}

	public static String probeContentType(String content) {
		Charset charset = StandardCharsets.UTF_8;
		byte[] byteArray = content.getBytes(charset);
		return new Tika().detect(byteArray);
	}

	public static String probeContentType(File file) throws IOException {
		return new Tika().detect(file);
	}

}
