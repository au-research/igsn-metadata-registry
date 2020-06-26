package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.entity.Schema;
import au.edu.ardc.igsn.repository.SchemaRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SchemaServiceTest {

    @MockBean
    private SchemaRepository repository;

    @Autowired
    SchemaService service;

    @Test
    public void it_can_find_1_schema() throws Exception {
        Schema actual = new Schema("test", "Test");
        when(repository.findById("test")).thenReturn(Optional.of(actual));

        assertThat(service.findById("test")).isNotNull();
        assertThat(service.findById("test").getName()).isEqualTo("Test");
    }

    @Test
    public void it_returns_null_if_it_cannot_find_a_schema()  {
        assertThat(service.findById("a-record-that-is-not-there")).isNull();
    }

    @Test
    public void it_can_create_a_schema() {
        Schema actual = new Schema("test", "Test");
        actual.setCreated(new Date());
        when(repository.save(any(Schema.class))).thenReturn(actual);

        Schema expected = service.create("test", "Test");
        assertThat(expected).isInstanceOf(Schema.class);
        assertThat(expected.getId()).isEqualTo("test");
        assertThat(expected.getName()).isEqualTo("Test");
        assertThat(expected.getCreated()).isNotNull();
        assertThat(expected.getCreated()).isBeforeOrEqualTo(new Date());
    }

    // TODO it_can_update_a_schema
    // TODO it_can_delete_a-schema

}