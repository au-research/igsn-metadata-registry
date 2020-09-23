package au.edu.ardc.registry.exception;

import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.provider.Metadata;

public class ContentProviderNotFoundException extends RuntimeException {

	public ContentProviderNotFoundException(Schema schema, Metadata metadata) {
		super(String.format("Metadata ( %s) provider Not Found for schema: %s", metadata.toString(), schema.getId()));
	}

}
