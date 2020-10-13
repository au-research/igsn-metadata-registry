package au.edu.ardc.registry.exception;

public class VersionContentAlreadyExisted extends APIException {

	private final String schema;
	private final String hash;

	public VersionContentAlreadyExisted(String schema, String hash) {
		super();
		this.schema = schema;
		this.hash = hash;

	}

	@Override
	public String getMessageID() {
		return "api.error.version-already-exist";
	}

	@Override
	public String[] getArgs() {
		return new String[] { this.schema, this.hash};
	}



}