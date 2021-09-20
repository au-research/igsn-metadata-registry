package au.edu.ardc.registry.common.model.schema;

import au.edu.ardc.registry.common.model.Schema;

public interface SchemaValidator {

	boolean validate(Schema schema, String payload);

}
