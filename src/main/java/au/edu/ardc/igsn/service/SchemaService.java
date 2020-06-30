package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import static java.lang.Boolean.parseBoolean;

/**
 * A Service that deals with supported Schema
 */
@Service
@PropertySource("classpath:schemas.properties")
public class SchemaService {

//    @Value("#{'${schemas.supported}'.split(',')}")
//    List<String> supportedSchemaIDs;

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
        if (!supportsSchema(schemaID)) {
            return null;
        }
        Schema schema = new Schema(schemaID);
        schema.setName(env.getProperty("schemas." + schemaID + ".name"));
        return schema;
    }

    public boolean supportsSchema(String schemaID) {

        // it exists in the supportedSchemaIDs
        List<String> supportedSchemaIDs = Arrays.asList(env.getProperty("schemas.supported").split(", "));

        // it is enabled
        String isEnabled = env.getProperty("schemas." + schemaID + ".enabled");

        // it has a name
        String name = env.getProperty("schemas." + schemaID + ".name");

        return supportedSchemaIDs.contains(schemaID) && parseBoolean(isEnabled) && !name.isEmpty();
    }

}
