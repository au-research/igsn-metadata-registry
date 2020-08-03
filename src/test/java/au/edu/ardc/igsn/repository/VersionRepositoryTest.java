package au.edu.ardc.igsn.repository;

import au.edu.ardc.igsn.TestHelper;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.entity.Version;
import org.apache.commons.codec.digest.DigestUtils;
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
import static org.assertj.core.api.InstanceOfAssertFactories.PATH;

@RunWith(SpringRunner.class)
@DataJpaTest
public class VersionRepositoryTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private VersionRepository versionRepository;

    @Autowired
    private RecordRepository recordRepository;


    @Test
    public void injectedComponentsAreNotNull() {
        assertThat(jdbcTemplate).isNotNull();
        assertThat(entityManager).isNotNull();
        assertThat(versionRepository).isNotNull();
    }

    @Test
    public void repository_can_find_by_id() {
        // given a version
        Record record = TestHelper.mockRecord();
        recordRepository.save(record);
        Version version = TestHelper.mockVersion(record);
        versionRepository.save(version);

        UUID id = version.getId();

        // when findById
        Optional<Version> dbFound = versionRepository.findById(id);

        // finds that version
        assertThat(dbFound.isPresent()).isTrue();

        Version found = dbFound.get();
        assertThat(found).isInstanceOf(Version.class);
        assertThat(found.getId()).isEqualTo(version.getId());
    }

    @Test
    public void can_find_existence_by_id() {
        // given a version
        Record record = TestHelper.mockRecord();
        recordRepository.save(record);
        Version version = TestHelper.mockVersion(record);
        versionRepository.save(version);

        UUID id = version.getId();

        assertThat(versionRepository.existsById(UUID.randomUUID())).isFalse();
        assertThat(versionRepository.existsById(id)).isTrue();
    }

    @Test
    public void existsByHash() {
        // given a version with a hash
        Record record = TestHelper.mockRecord();
        entityManager.persistAndFlush(record);
        Version version = TestHelper.mockVersion(record);
        String hash = DigestUtils.sha1Hex(version.getContent());
        version.setHash(hash);
        entityManager.persistAndFlush(version);

        // existsByHash is correct
        assertThat(versionRepository.existsByHash(hash)).isTrue();
        assertThat(versionRepository.existsByHash(DigestUtils.sha1Hex("some random string"))).isFalse();
    }

    @Test
    public void existsBySchemaAndHash() {
        // given a version with a hash
        Record record = TestHelper.mockRecord();
        entityManager.persistAndFlush(record);
        Version version = TestHelper.mockVersion(record);
        String schema = version.getSchema();
        String hash = DigestUtils.sha1Hex(version.getContent());
        version.setHash(hash);
        entityManager.persistAndFlush(version);

        // existsBySchemaAndHash is correct
        assertThat(versionRepository.existsBySchemaAndHash(schema, hash)).isTrue();
        assertThat(versionRepository.existsBySchemaAndHash(schema, DigestUtils.sha1Hex("some random string"))).isFalse();
    }

    @Test
    public void existsBySchemaAndHashAndCurrent() {
        // given a version with a hash
        Record record = TestHelper.mockRecord();
        entityManager.persistAndFlush(record);
        Version version = TestHelper.mockVersion(record);
        version.setCurrent(true);
        String schema = version.getSchema();
        String hash = DigestUtils.sha1Hex(version.getContent());
        version.setHash(hash);
        entityManager.persistAndFlush(version);

        assertThat(versionRepository.existsBySchemaAndHashAndCurrent(schema, hash, true)).isTrue();
        assertThat(versionRepository.existsBySchemaAndHashAndCurrent(schema, hash, false)).isFalse();
        assertThat(versionRepository.existsBySchemaAndHashAndCurrent(schema, DigestUtils.sha1Hex("some random string"), true)).isFalse();
    }
}