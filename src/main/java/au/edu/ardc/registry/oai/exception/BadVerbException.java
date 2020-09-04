package au.edu.ardc.registry.oai.exception;

import clover.org.apache.log4j.spi.ErrorCode;

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
