package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.Schema;
import au.edu.ardc.igsn.util.Helpers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SchemaServiceTest {

    @Autowired
    SchemaService service;

    @Test
    public void it_should_show_all_supported_schemas() {
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
    public void it_should_find_schema_by_id() {
        Schema schema = service.getSchemaByID("csiro-igsn-v3");
        assertThat(schema).isInstanceOf(Schema.class);
        assertThat(schema.getName()).isNotNull();
    }

    @Test
    public void it_should_support_schema() {
        assertThat(service.supportsSchema("csiro-igsn-v3")).isTrue();
        assertThat(service.supportsSchema("non-exist")).isFalse();
    }

    @Test
    public void a_schema_can_be_validated() throws IOException {
        // validate csiro-igsn-v3
        Schema csiroigsnv3 = service.getSchemaByID("csiro-igsn-v3");

        // given an xml as a string
        String xml = Helpers.readFile("src/test/resources/xml/sample_igsn_csiro_v3.xml");

        // validate it
        assertThat(service.validate(csiroigsnv3, xml)).isTrue();

        // validate invalid xml
        String invalidXML = Helpers.readFile("src/test/resources/xml/invalid_sample_igsn_csiro_v3.xml");
        assertThat(service.validate(csiroigsnv3, invalidXML)).isFalse();
    }

}