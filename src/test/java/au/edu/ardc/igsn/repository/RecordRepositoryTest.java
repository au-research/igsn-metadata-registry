package au.edu.ardc.igsn.repository;

import au.edu.ardc.igsn.entity.Record;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
public class RecordRepositoryTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private RecordRepository repository;

    @Test
    public void injectedComponentsAreNotNull() {
        assertThat(jdbcTemplate).isNotNull();
        assertThat(entityManager).isNotNull();
        assertThat(repository).isNotNull();
    }

    @Test
    public void canFindAll() {
        // given a record
        Record record = new Record();
        repository.save(record);

        // when findsAll, finds 1
        assertThat(repository.findAll()).hasSize(1);

        // adds another record
        Record record2 = new Record();
        repository.save(record2);

        // when findAll, finds 2
        assertThat(repository.findAll()).hasSize(2);
    }

    @Test
    public void repository_can_findById() {
        // given a record
        Record record = new Record();
        repository.save(record);

        UUID id = record.getId();

        // when findById
        Optional<Record> dbFound = repository.findById(id);

        // finds that record
        assertThat(dbFound.isPresent()).isTrue();

        Record found = dbFound.get();
        assertThat(found).isInstanceOf(Record.class);
        assertThat(found).isExactlyInstanceOf(Record.class);
        assertThat(found.getId()).isEqualTo(record.getId());
    }

    @Test
    public void it_can_find_all_records_created_by_a_user() {
        // given 3 records owned by Jack
        UUID jackUUID = UUID.randomUUID();
        UUID janeUUID = UUID.randomUUID();
        for (int i = 0; i < 3; i++) {
            Record record = new Record();
            record.setCreatorID(jackUUID);
            repository.save(record);
        }

        // and 2 records owned by Jane
        for (int i = 0; i < 2; i++) {
            Record record = new Record();
            record.setCreatorID(janeUUID);
            repository.save(record);
        }

        // Jack has 3 records
        assertThat(repository.findByCreatorID(jackUUID)).hasSize(3);

        // Jane has 2 records
        assertThat(repository.findByCreatorID(janeUUID)).hasSize(2);
    }
}
