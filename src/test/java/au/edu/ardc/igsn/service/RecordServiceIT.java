package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.repository.RecordRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class RecordServiceIT {

    @Autowired
    RecordService service;

    @Autowired
    RecordRepository repository;

    @Test
    public void it_can_update_a_record() {
        // given a record
        Record record = new Record();
        record.setCreatedAt(new Date());
        record = repository.save(record);

        // setting up a modified object with the same id
        UUID allocationID, modifiedBy;
        Record modified = service.findById(record.getId().toString());
        modified.setAllocationID(allocationID = UUID.randomUUID());

        // when update with the modified object
        service.update(modified, modifiedBy = UUID.randomUUID());

        // record is updated with the new allocationID
        Record actual = service.findById(record.getId().toString());
        assertThat(actual).isNotNull();
        assertThat(actual.getAllocationID()).isEqualTo(allocationID);
        assertThat(actual.getModifierID()).isEqualTo(modifiedBy);
        assertThat(actual.getCreatedAt()).isEqualTo(record.getCreatedAt());
        assertThat(actual.getModifiedAt()).isAfter(record.getCreatedAt());
    }

    @Test
    public void it_can_delete_a_record() {
        // given a record
        Record record = new Record();
        record.setCreatedAt(new Date());
        record = repository.save(record);
        String uuid = record.getId().toString();

        // it exists
        assertThat(service.findById(uuid)).isNotNull();
        assertThat(service.exists(uuid)).isTrue();

        // when delete
        boolean result = service.delete(record);

        // it returns truthy and it's gone
        assertThat(result).isTrue();
        assertThat(service.findById(uuid)).isNull();
        assertThat(service.exists(uuid)).isFalse();
    }

    @Test
    public void it_can_detect_existence_of_a_record_via_id() {
        // random uuid doesn't exist
        assertThat(service.exists(UUID.randomUUID().toString())).isFalse();

        // when a record is created
        Record record = repository.save(new Record());

        // it exists
        assertThat(service.exists(record.getId().toString())).isTrue();
    }

    @Test
    public void it_can_delete_the_record() {
        // given a record and it exists
        Record record = new Record();
        record.setCreatedAt(new Date());
        record = repository.save(record);
        UUID uuid = record.getId();
        assertThat(service.exists(uuid.toString())).isTrue();

        // when delete
        boolean result = service.delete(record);

        // it returns true and it's gone
        assertThat(result).isTrue();
        assertThat(service.exists(uuid.toString())).isFalse();
    }

}
