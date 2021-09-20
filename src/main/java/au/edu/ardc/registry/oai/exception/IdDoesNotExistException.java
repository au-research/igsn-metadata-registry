package au.edu.ardc.registry.oai.exception;

public class IdDoesNotExistException extends OAIException {

	public IdDoesNotExistException() {
		super();
	}

	@Override
	public String getMessageID() {
		return "oai.error.id-does-not-exist";
	}

	@Override
	public String getCode() {
		return OAIException.idDoesNotExistCode;
	}

}
