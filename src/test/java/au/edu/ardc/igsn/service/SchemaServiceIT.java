package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.entity.Schema;
import au.edu.ardc.igsn.repository.SchemaRepository;
import au.edu.ardc.igsn.util.Helpers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class SchemaServiceIT {

    @Autowired
    SchemaService service;

    @Autowired
    SchemaRepository repository;

    @Test
    @Transactional
    public void it_can_create_a_schema() {
        // when a schema is created
        Schema actual = service.create("test", "Test");
        assertThat(actual).isInstanceOf(Schema.class);

        // record is now retrievable
        Schema expected = service.findById("test");
        assertThat(expected).isInstanceOf(Schema.class);

        // the details match up
        assertThat(expected.getId()).isEqualTo("test");
        assertThat(expected.getName()).isEqualTo("Test");

        // creation date is set
        assertThat(expected.getCreated()).isNotNull();
        assertThat(expected.getCreated()).isBeforeOrEqualTo(new Date());
    }

    @Test
    @Transactional
    public void it_can_create_a_schema_with_a_provided_schema() {
        // when a schema is created
        Schema actual = service.create(new Schema("test", "Test"));
        assertThat(actual).isInstanceOf(Schema.class);

        // record is now retrievable
        Schema expected = service.findById("test");
        assertThat(expected).isInstanceOf(Schema.class);

        // the details match up
        assertThat(expected.getId()).isEqualTo("test");
        assertThat(expected.getName()).isEqualTo("Test");

        // creation date is set
        assertThat(expected.getCreated()).isNotNull();
        assertThat(expected.getCreated()).isBeforeOrEqualTo(new Date());
    }

    @Test
    @Transactional
    public void it_can_update_a_schema(){
        // given a schema
        Schema actual = new Schema("test", "Original");
        repository.save(actual);
        Schema expected = service.findById("test");
        assertThat(expected.getName()).isEqualTo("Original");

        // when update
        service.update(new Schema("test", "Updated"));

        // the field is updated
        expected = service.findById("test");
        assertThat(expected.getName()).isEqualTo("Updated");
    }

    @Test
    @Transactional
    public void it_can_delete_a_schema(){
        // given a schema
        Schema actual = new Schema("test", "Original");
        repository.save(actual);
        Schema expected = service.findById("test");
        assertThat(expected.getName()).isEqualTo("Original");

        // when delete
        Boolean result = service.delete("test");
        assertThat(result).isTrue();

        // and it's not found anymore
        expected = service.findById("test");
        assertThat(expected).isNull();
    }

    @Test
    public void delete_an_unknown_schema_returns_false() {
        assertThat(service.delete("unknown-schema")).isFalse();
    }

    @Test
    @Transactional
    public void it_can_validate_a_schema_using_local_path() throws IOException {
        // given an xml as a string
        String xml = Helpers.readFile("src/test/resources/xml/sample_igsn_csiro_v3.xml");

        // and a csiro-v3 schema
        Schema schema = new Schema("igsn-csiro-v3.0", "IGSN CSIRO version 3.0");
        schema.setLocal_path("schemas/igsn-csiro-v3.0/igsn-csiro-v3.0.xsd");
        service.create(schema);

        // sample IGSN is validated
        assertThat(service.validate(xml, schema)).isTrue();
    }

}