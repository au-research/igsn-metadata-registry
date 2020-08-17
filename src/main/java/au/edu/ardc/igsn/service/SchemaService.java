package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.model.Schema;
import au.edu.ardc.igsn.util.Helpers;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
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

    protected final String schemaConfigLocation = "src/main/resources/schemas.json";

    Logger logger = LoggerFactory.getLogger(SchemaService.class);
    private List<Schema> schemas;

    public void loadSchemas() throws Exception {
        logger.debug("Loading schema configuration from {}", schemaConfigLocation);
        String data = Helpers.readFile(schemaConfigLocation);
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
    @Cacheable("schema")
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

    @Cacheable("schemas")
    public List<Schema> getSchemas() {
        return schemas;
    }

    public void setSchemas(List<Schema> schemas) {
        this.schemas = schemas;
    }

    public boolean validate(Schema schema, String payload) {
        // todo validate(Schema, payload)
        return true;
    }
}
