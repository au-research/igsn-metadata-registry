package au.edu.ardc.registry.common.model.schema;

import au.edu.ardc.registry.common.model.Schema;

public class PlainTextValidator implements SchemaValidator{
    @Override
    public boolean validate(Schema schema, String payload) {
        // if it's a plain text document it should contain one or multiple lines of identifiers
        String[] lines = payload.split("\\r?\\n");
        return lines.length > 0;
    }
}
