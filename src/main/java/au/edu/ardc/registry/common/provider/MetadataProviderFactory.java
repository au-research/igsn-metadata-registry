package au.edu.ardc.registry.common.provider;

import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.exception.ContentProviderNotFoundException;

public class MetadataProviderFactory {

	public static Object create(Schema schema, Metadata metadata) throws ContentProviderNotFoundException {
		try {
			String fqdn = schema.getProviders().get(metadata);
			return Class.forName(fqdn).newInstance();
		}
		catch (NullPointerException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			throw new ContentProviderNotFoundException(schema, metadata);
		}
	}

}
