package au.edu.ardc.registry.common.transform;

import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.exception.ContentProviderNotFoundException;
import au.edu.ardc.registry.exception.TransformerNotFoundException;

public class TransformerFactory {


	public static Object create(Schema fromSchema, Schema toSchema) {
		try {
			String fqdn = fromSchema.getTransforms().get(toSchema.getId());
			return Class.forName(fqdn).newInstance();
		}
		catch (NullPointerException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			throw new TransformerNotFoundException(fromSchema.getId(), toSchema.getId());
		}
	}



}
