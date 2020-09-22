package au.edu.ardc.registry.exception;

public class SchemaNotSupportedException extends APIException {

	private final String schemaID;

	public SchemaNotSupportedException(String schemaID) {
		super();
		this.schemaID = schemaID;
	}

	@Override
	public String[] getArgs() {
		return new String[] { this.schemaID };
	}

	@Override
	public String getMessageID() {
		return "api.error.schema-not-supported";
	}

}
