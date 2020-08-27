package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.model.Schema;
import au.edu.ardc.igsn.model.schema.JSONSchema;
import au.edu.ardc.igsn.model.schema.XMLSchema;
import au.edu.ardc.igsn.util.Helpers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SchemaService.class)
class SchemaServiceTest {

    @Autowired
    SchemaService service;

    @Test
    void load() throws Exception {
        service.loadSchemas();
        assertThat(service.getSchemas()).isNotNull();
    }

    @Test
    void getSchemas() {
        // schemas are loaded @PostConstruct so all should be available
        assertThat(service.getSchemas()).extracting("class").contains(JSONSchema.class, XMLSchema.class);
    }

    @Test
    void getSchemaByID() {
        assertThat(service.getSchemaByID(SchemaService.ARDCv1)).isNotNull();
        Schema schema = service.getSchemaByID(SchemaService.ARDCv1);
        assertThat(schema).isInstanceOf(Schema.class);
        assertThat(schema.getName()).isNotNull();
    }

    @Test
    void supports() {
        assertThat(service.supportsSchema("ardc-igsn-desc-1.0")).isTrue();
        assertThat(service.supportsSchema("csiro-igsn-desc-3.0")).isTrue();
        assertThat(service.supportsSchema("igsn-desc-1.0")).isTrue();
        assertThat(service.supportsSchema("igsn-reg-1.0")).isTrue();
        assertThat(service.supportsSchema("non-exist")).isFalse();
    }

    @Test
    void validate_validARDCv1_true() throws Exception {
        Schema schema = service.getSchemaByID(SchemaService.ARDCv1);
        String validXML = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
        assertThat(service.validate(schema, validXML)).isTrue();
    }

    @Test
    void validate_validCSIROv3_true() throws Exception {
        Schema schema = service.getSchemaByID(SchemaService.CSIROv3);
        String validXML = Helpers.readFile("src/test/resources/xml/sample_igsn_csiro_v3.xml");
        assertThat(service.validate(schema, validXML)).isTrue();
    }
    
    @Test
    void getSchemaByNameSpace_ARDC() {
    	XMLSchema xs = service.getSchemaByNameSpace("https://identifiers.ardc.edu.au/schemas/ardc-igsn-desc");
    	assertThat(xs.getId().equals(SchemaService.ARDCv1));
    }
    
    @Test
    void getSchemaByNameSpace_CS() {
    	XMLSchema xs = service.getSchemaByNameSpace("https://igsn.csiro.au/schemas/3.0");
    	assertThat(xs.getId().equals(SchemaService.ARDCv1));
    }
}