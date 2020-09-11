package au.edu.ardc.registry.exception;

public class APIException extends RuntimeException {

	public APIException() {
		super();
	}

	public APIException(String msg) {
		super(msg);
	}

	public String[] getArgs() {
		return new String[] {};
	}

	public String getMessageID() {
		return "";
	}

}
