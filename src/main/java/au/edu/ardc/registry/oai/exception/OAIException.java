package au.edu.ardc.registry.oai.exception;

import au.edu.ardc.registry.exception.APIException;

public abstract class OAIException extends APIException {

	public static final String badArgumentCode = "badArgument";
	public static final String cannotDisseminateFormatCode = "cannotDisseminateFormat";

	public OAIException() {
		super();
	}

	public abstract String getCode();

}
