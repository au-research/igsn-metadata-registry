package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.entity.SchemaEntity;
import au.edu.ardc.igsn.repository.SchemaRepository;
import au.edu.ardc.igsn.util.Helpers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SchemaEntityServiceTest {

    @MockBean
    private SchemaRepository repository;

    @Autowired
    SchemaEntityService service;

    @Test
    public void it_can_find_1_schema() throws Exception {
        SchemaEntity actual = new SchemaEntity("test", "Test");
        when(repository.findById("test")).thenReturn(Optional.of(actual));

        assertThat(service.findById("test")).isNotNull();
        assertThat(service.findById("test").getName()).isEqualTo("Test");
    }

    @Test
    public void it_returns_null_if_it_cannot_find_a_schema()  {
        assertThat(service.findById("a-record-that-is-not-there")).isNull();
    }

    @Test
    public void it_can_create_a_schema_from_id_and_name() {

        //  mock response from repository
        SchemaEntity actual = new SchemaEntity("test", "Test");
        actual.setCreated(new Date());
        when(repository.save(any(SchemaEntity.class))).thenReturn(actual);

        SchemaEntity expected = service.create("test", "Test");
        assertThat(expected).isInstanceOf(SchemaEntity.class);
        assertThat(expected.getId()).isEqualTo("test");
        assertThat(expected.getName()).isEqualTo("Test");
        assertThat(expected.getCreated()).isNotNull();
        assertThat(expected.getCreated()).isBeforeOrEqualTo(new Date());
    }

    @Test
    public void it_can_create_a_schema() {

        // mock response from repository
        SchemaEntity actual = new SchemaEntity("test", "Test");
        actual.setCreated(new Date());
        when(repository.save(any(SchemaEntity.class))).thenReturn(actual);

        // when a schema is created
        SchemaEntity expected = service.create(new SchemaEntity("test", "Test"));
        assertThat(expected).isInstanceOf(SchemaEntity.class);
        assertThat(expected.getId()).isEqualTo("test");
        assertThat(expected.getName()).isEqualTo("Test");
        assertThat(expected.getCreated()).isNotNull();
        assertThat(expected.getCreated()).isBeforeOrEqualTo(new Date());
    }

    @Test
    public void it_can_update_a_schema() {

        // mock response from repository
        when(repository.save(any(SchemaEntity.class))).thenReturn(new SchemaEntity("test", "Updated"));

        // when a schema is updated
        service.update(new SchemaEntity("test", "Updated"));
    }

    // TODO it_can_delete_a-schema

    @Test
    public void it_can_validate_a_schema_using_local_path() throws IOException {
        // given an xml as a string
        String xml = Helpers.readFile("src/test/resources/xml/sample_igsn_csiro_v3.xml");

        // and a csiro-v3 schema
        SchemaEntity schema = new SchemaEntity("igsn-csiro-v3.0", "IGSN CSIRO version 3.0");
        schema.setLocal_path("schemas/igsn-csiro-v3.0/igsn-csiro-v3.0.xsd");

        // sample IGSN is validated
        assertThat(service.validate(xml, schema)).isTrue();
    }

}