package au.edu.ardc.igsn.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
class RecordTest {

    @Autowired
    TestEntityManager entityManager;

    @Test
    void a_record_have_auto_generated_uuid() {

        // given a saved record
        Record record = new Record();
        entityManager.persistAndFlush(record);

        // uuid is generated and is the correct format
        assertThat(record.getId()).isNotNull();
        assertThat(record.getId()).isInstanceOf(UUID.class);
        assertThat(record.getId().toString()).matches("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})");
    }

    @Test
    void a_record_has_enumerated_status() {
        Record record = new Record();
        record.setStatus(Record.Status.PUBLISHED);

        entityManager.persistAndFlush(record);
        assertThat(record.getStatus()).isEqualTo(Record.Status.PUBLISHED);
    }
}