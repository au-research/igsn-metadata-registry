package au.edu.ardc.registry.oai.exception;

import au.edu.ardc.registry.exception.APIException;

public class CannotDisseminateFormatException extends OAIException {

	public CannotDisseminateFormatException() {
		super();
	}

	@Override
	public String getMessageID() {
		return "oai.error.cannot-disseminate-format";
	}

	@Override
	public String getCode() {
		return OAIException.cannotDisseminateFormatCode;
	}

}
