package au.edu.ardc.registry.exception;

import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.provider.Metadata;

public class ContentProviderNotFoundException extends APIException {

	private final String schema;

	private final String metadata;

	public ContentProviderNotFoundException(Schema schema, Metadata metadata) {
		super();
		this.schema = schema.getId();
		this.metadata = metadata.toString();
	}

	@Override
	public String getMessageID() {
		return "api.error.content_not_supported";
	}

	@Override
	public String[] getArgs() {
		return new String[] { this.schema, this.metadata };
	}

}
