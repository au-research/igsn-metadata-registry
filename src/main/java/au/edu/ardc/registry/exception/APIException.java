package au.edu.ardc.registry.exception;

public class APIException extends RuntimeException {

	public String[] getArgs() {
		return new String[] {};
	}

	public String getMessageID() {
		return "";
	}

}
