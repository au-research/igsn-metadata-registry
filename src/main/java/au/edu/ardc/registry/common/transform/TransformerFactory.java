package au.edu.ardc.registry.common.transform;

import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.exception.TransformerNotFoundException;

public class TransformerFactory {

	public static Object create(Schema fromSchema, Schema toSchema) {
		try {
			String fqn = fromSchema.getTransforms().get(toSchema.getId());
			return Class.forName(fqn).newInstance();
		}
		catch (Exception e) {
			throw new TransformerNotFoundException(fromSchema.getId(), toSchema.getId());
		}
	}

}
