package au.edu.ardc.registry.common.repository;

import au.edu.ardc.registry.TestHelper;
import au.edu.ardc.registry.common.entity.URL;
import au.edu.ardc.registry.common.entity.Record;
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
public class URLRepositoryTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private URLRepository urlRepository;

    @Autowired
    private RecordRepository recordRepository;

    @Test
    public void injectedComponentsAreNotNull() {
        assertThat(jdbcTemplate).isNotNull();
        assertThat(entityManager).isNotNull();
        assertThat(urlRepository).isNotNull();
    }

    @Test
    public void repository_can_find_by_id() {
        // given a url
        Record record = TestHelper.mockRecord();
        recordRepository.save(record);
        URL url = TestHelper.mockUrl(record);
        urlRepository.save(url);

        UUID id = url.getId();

        // when findById
        Optional<URL> dbFound = urlRepository.findById(id);

        // finds that url
        assertThat(dbFound.isPresent()).isTrue();

        URL found = dbFound.get();
        assertThat(found).isInstanceOf(URL.class);
        assertThat(found.getId()).isEqualTo(url.getId());
    }
}
