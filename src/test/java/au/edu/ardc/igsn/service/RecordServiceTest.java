package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.repository.RecordRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RecordServiceTest {

    @Autowired
    private RecordService service;

    @MockBean
    private RecordRepository repository;

    @Test
    public void it_can_find_1_record_by_id() {
        // given a record
        String uuid = UUID.randomUUID().toString();
        Record expected = new Record(uuid);
        when(repository.findById(uuid)).thenReturn(Optional.of(expected));

        // when findById
        Record actual = service.findById(uuid.toString());

        // returns the same record
        assertThat(actual).isNotNull();
        assertThat(actual.getId()).isEqualTo(expected.getId());
    }

    @Test
    public void it_can_find_all_records_by_creator_id() {
        ArrayList<Record> records = new ArrayList<>();

        // given a random number of records
        int max = (int) (Math.random() * 100);
        for (int i = 0; i < max; i++) {
            Record record = new Record();
            records.add(record);
        }
        when(repository.findByCreatedBy(anyString())).thenReturn(records);

        // when find by creator, returns the same number of those records
        assertThat(service.findByCreatorID("something")).hasSize(max);
    }

    @Test
    public void it_can_tell_if_a_record_exists_by_id() {
        when(repository.existsById(anyString())).thenReturn(true);
        assertThat(service.exists(anyString())).isTrue();
    }

    @Test
    public void it_can_create_a_new_record() {
        String creatorUUID, recordUUID, allocationUUID;
        Record expected = new Record(recordUUID = UUID.randomUUID().toString());
        expected.setCreatedBy(creatorUUID = UUID.randomUUID().toString());
        expected.setAllocationID(allocationUUID = UUID.randomUUID().toString());
        expected.setOwnerType(Record.OwnerType.User);

        when(repository.save(any(Record.class))).thenReturn(expected);

        Record actual = service.create(creatorUUID, allocationUUID, Record.OwnerType.User);
        assertThat(actual).isNotNull();
        assertThat(actual).isInstanceOf(Record.class);
        assertThat(actual.getId()).isEqualTo(recordUUID);
    }

}