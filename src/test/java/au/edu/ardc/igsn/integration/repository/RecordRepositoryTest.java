package au.edu.ardc.igsn.integration.repository;

import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.repository.RecordRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManager;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
public class RecordRepositoryTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private RecordRepository recordRepository;

    @Test
    public void injectedComponentsAreNotNull() {
        assertThat(jdbcTemplate).isNotNull();
        assertThat(entityManager).isNotNull();
        assertThat(recordRepository).isNotNull();
    }

    @Test
    public void canFindAll() {
        // given a record
        Record record = new Record();
        recordRepository.save(record);

        // when findsAll, finds 1
        assertThat(recordRepository.findAll()).hasSize(1);

        // adds another record
        Record record2 = new Record();
        recordRepository.save(record2);

        // when findAll, finds 2
        assertThat(recordRepository.findAll()).hasSize(2);
    }

    @Test
    public void canFindById() {
        // given a record
        Record record = new Record();
        recordRepository.save(record);

        String id = record.getId();

        // when findById
        Optional<Record> dbFound = recordRepository.findById(id);

        // finds that record
        assertThat(dbFound.isPresent()).isTrue();

        Record found = dbFound.get();
        assertThat(found).isInstanceOf(Record.class);
        assertThat(found).isExactlyInstanceOf(Record.class);
        assertThat(found.getId()).isEqualTo(record.getId());
    }
}
