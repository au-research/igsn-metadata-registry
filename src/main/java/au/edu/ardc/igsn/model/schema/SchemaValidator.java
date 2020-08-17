package au.edu.ardc.igsn.model.schema;

import au.edu.ardc.igsn.model.Schema;

public interface SchemaValidator {
    boolean validate(Schema schema, String payload);
}
