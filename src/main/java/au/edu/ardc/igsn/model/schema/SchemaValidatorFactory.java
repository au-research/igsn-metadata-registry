package au.edu.ardc.igsn.model.schema;

import au.edu.ardc.igsn.model.Schema;

public class SchemaValidatorFactory {
    public static SchemaValidator getValidator(Schema schema) {
        if (schema.getClass().equals(XMLSchema.class)) {
            return new XMLValidator();
        }

        // todo JSONValidator

        return null;
    }
}
