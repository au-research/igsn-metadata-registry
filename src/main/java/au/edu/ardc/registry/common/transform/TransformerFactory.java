package au.edu.ardc.registry.common.transform;

import au.edu.ardc.registry.common.model.Schema;

public class TransformerFactory {

	public static Object create(Schema fromSchema, Schema toSchema) {
		try {
			String fqn = fromSchema.getTransforms().get(toSchema.getId());
			return Class.forName(fqn).newInstance();
		}
		catch (Exception e) {
			// todo throw special exception for this
			e.printStackTrace();
			return null;
		}
	}

}
