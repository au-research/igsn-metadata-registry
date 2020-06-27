package au.edu.ardc.igsn.entity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
public class RecordTest {

    @Autowired
    TestEntityManager entityManager;

    @Test
    public void a_record_have_auto_generated_uuid() {

        // given a saved record
        Record record = new Record();
        entityManager.persistAndFlush(record);

        // uuid is generated and is the correct format
        assertThat(record.getId()).isNotNull();
        assertThat(record.getId()).matches("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})");
    }
}