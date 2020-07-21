package au.edu.ardc.igsn.repository;

import au.edu.ardc.igsn.TestHelper;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.entity.Identifier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
public class IdentifierRepositoryTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private IdentifierRepository identifierRepository;

    @Autowired
    private RecordRepository recordRepository;

    @Test
    public void injectedComponentsAreNotNull() {
        assertThat(jdbcTemplate).isNotNull();
        assertThat(entityManager).isNotNull();
        assertThat(identifierRepository).isNotNull();
    }

    @Test
    public void repository_can_find_by_id() {
        // given an identifier
        Record record = TestHelper.mockRecord();
        recordRepository.save(record);
        Identifier identifier = TestHelper.mockIdentifier(record);
        identifierRepository.save(identifier);

        UUID id = identifier.getId();

        // when findById
        Optional<Identifier> dbFound = identifierRepository.findById(id);

        // finds that version
        assertThat(dbFound.isPresent()).isTrue();

        Identifier found = dbFound.get();
        assertThat(found).isInstanceOf(Identifier.class);
        assertThat(found.getId()).isEqualTo(identifier.getId());
    }
}
