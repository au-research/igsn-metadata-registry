package au.edu.ardc.registry.oai.exception;

public class BadVerbException extends RuntimeException {

	private static String code;

	public BadVerbException(String message, String oaiCode) {
		super(message);
		code = oaiCode;
	}

	public static String getCode() {
		return code;
	}

}
