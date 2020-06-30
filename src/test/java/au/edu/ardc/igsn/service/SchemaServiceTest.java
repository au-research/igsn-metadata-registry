package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.Schema;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

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
        assertThat(supported).allSatisfy( schema -> {
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

}