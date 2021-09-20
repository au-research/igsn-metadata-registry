package au.edu.ardc.registry.oai.exception;

public class NoRecordsMatchException extends OAIException {

	public NoRecordsMatchException() {
		super();
	}

	@Override
	public String getMessageID() {
		return "oai.error.no-records-match";
	}

	@Override
	public String getCode() {
		return OAIException.noRecordsMatchCode;
	}

}
