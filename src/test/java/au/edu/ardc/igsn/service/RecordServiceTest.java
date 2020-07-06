package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.TestHelper;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.repository.RecordRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RecordServiceTest {

    @Autowired
    private RecordService service;

    @MockBean
    private RecordRepository repository;

    @MockBean
    private KeycloakService kcService;

    @Test
    public void it_can_find_1_record_by_id() {
        // given a record
        UUID uuid = UUID.randomUUID();
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
        UUID creatorID = UUID.randomUUID();

        // given a random number of records
        int max = (int) (Math.random() * 100);
        for (int i = 0; i < max; i++) {
            Record record = new Record();
            record.setCreatorID(creatorID);
            records.add(record);
        }
        when(repository.findByCreatorID(creatorID)).thenReturn(records);

        // when find by creator, returns the same number of those records
        assertThat(service.findByCreatorID(creatorID.toString())).hasSize(max);
    }

    @Test
    public void it_can_tell_if_a_record_exists_by_id() {
        UUID randomUUID = UUID.randomUUID();
        when(repository.existsById(randomUUID)).thenReturn(true);
        assertThat(service.exists(randomUUID.toString())).isTrue();
    }

    @Test
    public void it_can_create_a_new_record() {
        UUID creatorUUID, recordUUID, allocationUUID;
        Record expected = new Record(recordUUID = UUID.randomUUID());
        expected.setCreatorID(creatorUUID = UUID.randomUUID());
        expected.setAllocationID(allocationUUID = UUID.randomUUID());
        expected.setOwnerType(Record.OwnerType.User);

        when(repository.save(any(Record.class))).thenReturn(expected);

        Record actual = service.create(creatorUUID, allocationUUID, Record.OwnerType.User, null);
        assertThat(actual).isNotNull();
        assertThat(actual).isInstanceOf(Record.class);
        assertThat(actual.getId()).isEqualTo(recordUUID);
    }

    @Test
    public void it_can_find_owned_records() {
        UUID creatorID = UUID.randomUUID();

        // given a record that is created
        Record record = new Record();
        record.setOwnerID(creatorID);
        List<Record> records = new ArrayList<>();
        records.add(record);

        // mock the current request out too
        // HttpServletRequest request = mock(HttpServletRequest.class);

        // when finding an owned record, it tries to obtain the userID from the context
        when(kcService.getUserUUID(any(HttpServletRequest.class))).thenReturn(creatorID);
        when(repository.findOwned(creatorID)).thenReturn(records);

        // ensure records are returned when findOwned
        assertThat(service.findOwned(creatorID)).contains(record);
    }

    @Test
    public void it_can_create_record_for_user_owner_type() {
        // setup
        Record actual = TestHelper.mockRecord();
        when(repository.save(any(Record.class))).thenReturn(actual);

        // given a creation of a new record
        Record expected = service.create(actual.getCreatorID(), actual.getAllocationID(), Record.OwnerType.User, null);

        verify(repository, times(1)).save(any(Record.class));

        // ensure record is set on all fields
        assertThat(expected.getCreatorID()).isEqualTo(actual.getCreatorID());
        assertThat(expected.getAllocationID()).isEqualTo(actual.getAllocationID());
        assertThat(expected.getCreatedAt()).isAfterOrEqualTo(actual.getCreatedAt());
        assertThat(expected.getUpdatedAt()).isAfterOrEqualTo(actual.getUpdatedAt());

        // the record owner type and owner id is set properly
        assertThat(expected.getOwnerType()).isEqualTo(Record.OwnerType.User);
        assertThat(expected.getOwnerID()).isEqualTo(actual.getCreatorID());
    }

    @Test
    public void it_can_create_record_for_datacenter_owner_type() {
        // setup
        Record actual = TestHelper.mockRecord();
        actual.setOwnerType(Record.OwnerType.DataCenter);
        actual.setOwnerID(actual.getDataCenterID());
        when(repository.save(any(Record.class))).thenReturn(actual);

        // given a creation of a new record
        Record expected = service.create(actual.getCreatorID(), actual.getAllocationID(), Record.OwnerType.DataCenter, actual.getDataCenterID());

        verify(repository, times(1)).save(any(Record.class));

        // ensure all fields are set
        assertThat(expected.getCreatorID()).isEqualTo(actual.getCreatorID());
        assertThat(expected.getAllocationID()).isEqualTo(actual.getAllocationID());
        assertThat(expected.getCreatedAt()).isAfterOrEqualTo(actual.getCreatedAt());
        assertThat(expected.getUpdatedAt()).isAfterOrEqualTo(actual.getUpdatedAt());

        // the record owner type and owner id is set properly
        assertThat(expected.getOwnerType()).isEqualTo(Record.OwnerType.DataCenter);
        assertThat(expected.getOwnerID()).isEqualTo(actual.getDataCenterID());
    }

    @Test
    public void it_updates_record_correctly() {
        Record actual = TestHelper.mockRecord();
        when(repository.save(any(Record.class))).thenReturn(actual);

        // when a record is updated with another user
        UUID anotherUserID = UUID.randomUUID();
        Record updated = service.update(actual, anotherUserID);

        // the save method is invoked on the repository
        verify(repository, times(1)).save(any(Record.class));

        // the updated record is returned with updated values
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(actual.getUpdatedAt());
        assertThat(updated.getModifierID()).isEqualTo(anotherUserID);
    }

    // todo delete

    @Test
    public void it_deletes_record() {
        service.delete(TestHelper.mockRecord());

        // invoke the delete method from repository
        verify(repository, times(1)).delete(any(Record.class));
    }

}