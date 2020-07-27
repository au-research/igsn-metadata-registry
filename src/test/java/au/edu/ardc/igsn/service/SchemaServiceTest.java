package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.model.Schema;
import au.edu.ardc.igsn.util.Helpers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SchemaService.class)
class SchemaServiceTest {

    @Autowired
    SchemaService service;

    @Test
    void it_should_show_all_supported_schemas() {
        // given all supported schemas
        List<Schema> supported = service.getSupportedSchemas();

        // ensure that all instance of schema are returned
        assertThat(supported).allSatisfy(schema -> {
            assertThat(schema).isInstanceOf(Schema.class);

            // schema must have a name
            assertThat(schema.getName()).isNotNull();
        });
    }

    @Test
    void it_should_find_schema_by_id() {
        Schema schema = service.getSchemaByID("igsn-csiro-v3-descriptive");
        assertThat(schema).isInstanceOf(Schema.class);
        assertThat(schema.getName()).isNotNull();
    }

    @Test
    void it_should_support_schema() {
        assertThat(service.supportsSchema("igsn-csiro-v3-descriptive")).isTrue();
        assertThat(service.supportsSchema("igsn-descriptive-v1")).isTrue();
        assertThat(service.supportsSchema("igsn-registration-v1")).isTrue();
        assertThat(service.supportsSchema("non-exist")).isFalse();
    }

    @Test
    void a_schema_can_be_validated() throws IOException {
        // validate igsn-csiro-v3-descriptive
        Schema csiroigsnv3 = service.getSchemaByID("igsn-csiro-v3-descriptive");

        // given an xml as a string
        String xml = Helpers.readFile("src/test/resources/xml/sample_igsn_csiro_v3.xml");

        // validate it
        assertThat(service.validate(csiroigsnv3, xml)).isTrue();

        // validate invalid xml
        String invalidXML = Helpers.readFile("src/test/resources/xml/invalid_sample_igsn_csiro_v3.xml");
        assertThat(service.validate(csiroigsnv3, invalidXML)).isFalse();
    }

    @Test
    void validate_igsn_registration_v1() throws IOException {
        Schema schema = service.getSchemaByID("igsn-registration-v1");
        String xml = Helpers.readFile("src/test/resources/xml/sample_igsn_descriptive_v1.xml");
        assertThat(service.validate(schema, xml)).isTrue();
    }

}