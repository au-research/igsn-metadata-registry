package au.edu.ardc.registry.oai.exception;

public class NoMetadataFormatsException extends OAIException {

	public NoMetadataFormatsException() {
		super();
	}

	@Override
	public String getMessageID() {
		return "oai.error.no-metadata-formats";
	}

	@Override
	public String getCode() {
		return OAIException.noMetadataFormatsCode;
	}

}
