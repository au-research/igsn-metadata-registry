package au.edu.ardc.igsn.entity;

import au.edu.ardc.igsn.TestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
public class VersionTest {

    @Autowired
    TestEntityManager entityManager;

    @Test
    public void a_version_should_have_auto_generated_uuid() {
        Record record = new Record();
        entityManager.persistAndFlush(record);
        Version version = TestHelper.mockVersion(record);
        entityManager.persistAndFlush(version);

        // uuid is generated and is the correct format
        assertThat(version.getId()).isNotNull();
        assertThat(version.getId()).isInstanceOf(UUID.class);
        assertThat(version.getId().toString()).matches("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})");
    }

    @Test(expected = javax.persistence.PersistenceException.class)
    public void throws_exception_when_saving_without_a_record() {
        Version version = new Version();
        entityManager.persistAndFlush(version);
    }

    @Test
    public void a_version_must_have_a_record() {
        Version version = TestHelper.mockVersion();
        assertThat(version.getRecord()).isInstanceOf(Record.class);
    }

    @Test
    public void a_version_must_have_dates() {
        Version version = TestHelper.mockVersion();
        assertThat(version.getCreatedAt()).isInstanceOf(Date.class);
        assertThat(version.getEndedAt()).isNull();
    }

    @Test
    public void a_version_must_have_creator() {
        Version version = TestHelper.mockVersion();
        assertThat(version.getCreatorID()).isInstanceOf(UUID.class);
    }

}