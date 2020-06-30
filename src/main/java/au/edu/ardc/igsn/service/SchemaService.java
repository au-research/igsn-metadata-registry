package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.Schema;
import au.edu.ardc.igsn.util.XMLValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static java.lang.Boolean.parseBoolean;

/**
 * A Service that deals with supported Schema
 */
@Service
@PropertySource("classpath:schemas.properties")
public class SchemaService {

    @Autowired
    private Environment env;

    /**
     * Get all supported schema
     *
     * @return list of supported schema
     */
    public List<Schema> getSupportedSchemas() {

        // TODO handle when schemas.supported is empty

        String[] supportedSchemaIDs = env.getProperty("schemas.supported").split(", ");

        List<Schema> supported = new LinkedList<>();
        for (String schemaID : supportedSchemaIDs) {
            if (supportsSchema(schemaID)) {
                supported.add(getSchemaByID(schemaID));
            }
        }

        return supported;
    }

    /**
     * Get a Schema by ID
     *
     * @param schemaID the ID of the supported Schema
     * @return Schema
     */
    public Schema getSchemaByID(String schemaID) {
        Schema schema = new Schema(schemaID);
        schema.setName(env.getProperty("schemas." + schemaID + ".name"));
        return schema;
    }

    /**
     * Tells if a schema by ID is currently supported by the system
     *
     * @param schemaID String schemaID
     * @return boolean
     */
    public boolean supportsSchema(String schemaID) {

        // it exists in the supportedSchemaIDs
        List<String> supportedSchemaIDs = Arrays.asList(env.getProperty("schemas.supported").split(", "));

        // it is enabled
        String isEnabled = env.getProperty("schemas." + schemaID + ".enabled");

        // it has a name
        String name = env.getProperty("schemas." + schemaID + ".name");

        return supportedSchemaIDs.contains(schemaID) && parseBoolean(isEnabled) && !name.isEmpty();
    }

    /**
     * Validate a payload against a schema
     *
     * @param schema  Schema
     * @param payload String payload
     * @return boolean validation result
     */
    public boolean validate(Schema schema, String payload) {
        // TODO handle JSON validation
        // TODO raise SchemaValidationException

        // validate XML schema
        String schemaPath = env.getProperty("schemas." + schema.getId() + ".path");
        return XMLValidator.validateXMLStringWithXSDPath(payload, schemaPath);
    }

}
