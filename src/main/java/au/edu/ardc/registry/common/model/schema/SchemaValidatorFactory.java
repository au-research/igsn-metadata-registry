package au.edu.ardc.registry.common.model.schema;

import java.io.IOException;

import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.util.Helpers;

public class SchemaValidatorFactory {
    public static SchemaValidator getValidator(Schema schema) {
        if (schema.getClass().equals(XMLSchema.class)) {
            return new XMLValidator();
        }
    	else if(schema.getClass().equals(JSONSchema.class)){
    		return new JSONValidator();
    	}

        return null;
    }
    
    
    public static SchemaValidator getValidator(String content) throws IOException {
    	if(Helpers.probeContentType(content) == "application/xml") {
    		return new XMLValidator();
    	}
    	else if(Helpers.probeContentType(content) == "application/json"){
    		return new JSONValidator();
    	}
    	return null;
    }
}
