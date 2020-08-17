package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.model.Schema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

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
        assertThat(service.getSchemas()).extracting("class").contains(Schema.class);
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

}