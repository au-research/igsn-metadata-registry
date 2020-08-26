package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.model.Schema;
import au.edu.ardc.igsn.model.schema.SchemaValidator;
import au.edu.ardc.igsn.model.schema.SchemaValidatorFactory;
import au.edu.ardc.igsn.util.Helpers;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * A Service that deals with supported Schema
 */
@Service
public class SchemaService {

    // useful helper constants
    public static final String ARDCv1 = "ardc-igsn-desc-1.0";
    public static final String IGSNDESCv1 = "igsn-desc-1.0";
    public static final String IGSNREGv1 = "igsn-desc-1.0";
    public static final String CSIROv3 = "csiro-igsn-desc-3.0";

    protected final String schemaConfigLocation = "schemas/schemas.json";

    Logger logger = LoggerFactory.getLogger(SchemaService.class);
    private List<Schema> schemas;

    /**
     * Loads all schemas into locally accessible schema
     * Go through the schemaConfigLocation file and loads map all available schema
     *
     * @throws Exception read file exception
     */
    public void loadSchemas() throws Exception {
        logger.debug("Loading schema configuration from {}", schemaConfigLocation);
        String data = Helpers.readFileOnClassPath("schemas.json");
        logger.debug("Loaded schema configuration, data length: {}", data.length());
        ObjectMapper mapper = new ObjectMapper();
        List<Schema> schemas = Arrays.asList(mapper.readValue(data, Schema[].class));
        logger.debug("Found & registered {} schemas", schemas.size());
        this.setSchemas(schemas);
    }

    @PostConstruct
    public void init() throws Exception {
        loadSchemas();
    }

    /**
     * Get a Schema by ID
     *
     * @param schemaID the ID of the supported Schema
     * @return Schema
     */
    public Schema getSchemaByID(String schemaID) {
        logger.debug("Load schema by ID {}", schemaID);
        Optional<Schema> found = this.getSchemas().stream()
                .filter(schema -> schema.getId().equals(schemaID)).findFirst();

        return found.orElse(null);
    }

    /**
     * Tells if a schema by ID is currently supported by the system
     *
     * @param schemaID String schemaID
     * @return boolean
     */
    public boolean supportsSchema(String schemaID) {
        return getSchemaByID(schemaID) != null;
    }

    public List<Schema> getSchemas() {
        return schemas;
    }

    /**
     * Sets the current schemas in memory
     *
     * @param schemas a List of Schema
     */
    public void setSchemas(List<Schema> schemas) {
        this.schemas = schemas;
    }

    /**
     * Validate a payload given a schema
     * Will autodetect the schema type and spool up a SchemaValidator accordingly
     * Supports XMLValidator current
     * todo support JSONValidator
     *
     * @param schema The Schema to validate against
     * @param payload the String payload to validate
     * @return true if validation success
     * @throws Exception throws exception for validator creation and validation
     */
    public boolean validate(Schema schema, String payload) throws Exception {
        // detect type of schema
        // todo refactor ValidatorFactory.getValidator(schema.getClass())

        SchemaValidator validator = SchemaValidatorFactory.getValidator(schema);
        if (validator == null) {
            throw new Exception(String.format("Validator for schema %s is not found", schema.getId()));
        }

        return validator.validate(schema, payload);
    }
}
