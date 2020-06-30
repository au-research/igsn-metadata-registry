package au.edu.ardc.igsn.repository;

import au.edu.ardc.igsn.entity.SchemaEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
public class SchemaEntityRepositoryTest {

    @Autowired
    private SchemaRepository repository;

    @Test
    public void repository_can_findAll() {
        // given 100 schemas
        for (int i = 0; i < 100; i++) {
            repository.save(new SchemaEntity("schema " + i, "Schema" + i));
        }

        // when findAll, got 100
        assertThat(repository.findAll()).hasSize(100);
    }

    @Test
    public void repository_can_findById() {
        // given a schema
        SchemaEntity actual = new SchemaEntity("csiro-v3-igsn", "CSIRO version 3");

        // when save
        repository.save(actual);

        // exists in the repository
        Optional<SchemaEntity> expectedOp = repository.findById("csiro-v3-igsn");
        assertThat(expectedOp.isPresent()).isTrue();
        SchemaEntity expected = expectedOp.get();
        assertThat(expected.getName()).isEqualTo(actual.getName());
        assertThat(expected.getId()).isEqualTo(actual.getId());
    }
}