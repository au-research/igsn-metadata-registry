package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.Scope;
import au.edu.ardc.igsn.TestHelper;
import au.edu.ardc.igsn.User;
import au.edu.ardc.igsn.dto.RecordDTO;
import au.edu.ardc.igsn.dto.RecordMapper;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.repository.RecordRepository;
import com.google.common.collect.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@EnableAutoConfiguration
@SpringBootTest
@Transactional
public class RecordServiceIT {

    @Autowired
    RecordService service;

    @Autowired
    RecordRepository repository;

    @Autowired
    RecordMapper mapper;

    @Test
    public void findById_recordExists_returnsDTO() {
        User user = TestHelper.mockUser();

        // given a record
        Record expected = TestHelper.mockRecord();
        repository.saveAndFlush(expected);

        // when findById
        RecordDTO actual = service.findById(expected.getId().toString(), user);

        // found the right record
        assertThat(actual).isNotNull();
        assertThat(actual).isInstanceOf(RecordDTO.class);
        assertThat(actual.getId()).isEqualTo(expected.getId());
    }

    @Test
    public void create_UserSufficientPermission_returnsDTO() {
        // given an allocation
        UUID allocationID = UUID.randomUUID();

        // given the user with proper permission
        User user = TestHelper.mockUser();
        TestHelper.addResourceAndScopePermissionToUser(user, allocationID.toString(), Sets.newHashSet(Scope.CREATE.getValue(), Scope.IMPORT.getValue()));

        // given the dto
        RecordDTO dto = new RecordDTO();
        dto.setAllocationID(allocationID);

        // when create
        RecordDTO result = service.create(dto, user);
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(RecordDTO.class);
        assertThat(result.getId()).isNotNull();

        // actualRecord has createdAt, creatorID, ownerType and ownerID
        Record actualRecord = service.findById(result.getId().toString());
        assertThat(actualRecord.getCreatedAt()).isNotNull();
        assertThat(actualRecord.getCreatorID()).isEqualTo(user.getId());
        assertThat(actualRecord.getOwnerType()).isEqualTo(Record.OwnerType.User);
        assertThat(actualRecord.getOwnerID()).isEqualTo(user.getId());
    }


    @Test
    public void create_UserImportScope_returnsNonDefaultDTO() throws ParseException {
        // given an allocation
        UUID allocationID = UUID.randomUUID();

        // given the user with proper permission
        User user = TestHelper.mockUser();
        TestHelper.addResourceAndScopePermissionToUser(
                user, allocationID.toString(),
                Sets.newHashSet(Scope.CREATE.getValue(), Scope.IMPORT.getValue())
        );

        // given the dto
        Date updatedCreatedAt = new SimpleDateFormat("dd/MM/yyyy").parse("02/02/1989");
        Date updatedModifiedAt = new SimpleDateFormat("dd/MM/yyyy").parse("03/03/1989");
        UUID updatedCreatorID = UUID.randomUUID();
        RecordDTO dto = new RecordDTO();
        dto.setAllocationID(allocationID);
        dto.setModifiedAt(updatedModifiedAt);
        dto.setCreatedAt(updatedCreatedAt);
        dto.setCreatorID(updatedCreatorID);

        // when create
        RecordDTO result = service.create(dto, user);
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(RecordDTO.class);
        assertThat(result.getId()).isNotNull();

        // actualRecord has createdAt, creatorID, ownerType and ownerID
        Record actualRecord = service.findById(result.getId().toString());
        assertThat(actualRecord.getCreatedAt()).isEqualTo(updatedCreatedAt);
        assertThat(actualRecord.getModifiedAt()).isEqualTo(updatedModifiedAt);
        assertThat(actualRecord.getCreatorID()).isEqualTo(updatedCreatorID);
    }

    @Test
    public void update_UserSufficientPermission_returnsDTO() {
        // given an allocation
        UUID allocationID = UUID.randomUUID();

        // given the user with proper permission
        User user = TestHelper.mockUser();
        TestHelper.addResourceAndScopePermissionToUser(user, allocationID.toString(), Sets.newHashSet(Scope.UPDATE.getValue(), Scope.IMPORT.getValue()));

        // given a record
        Record record = TestHelper.mockRecord();
        record.setStatus(Record.Status.DRAFT);
        record.setCreatedAt(new Date());
        record.setAllocationID(allocationID);
        record = repository.saveAndFlush(record);

        // when update with a dto to change
        RecordDTO dto = new RecordDTO();
        dto.setId(record.getId());
        dto.setStatus(Record.Status.PUBLISHED);

        // when update with the modified object
        RecordDTO resultDTO = service.update(dto, user);

        // the result dto is the same as the object
        assertThat(resultDTO).isInstanceOf(RecordDTO.class);

        // record is updated with the new status
        Record actual = service.findById(record.getId().toString());
        assertThat(actual).isNotNull();
        assertThat(actual.getStatus()).isEqualTo(Record.Status.PUBLISHED);
    }

    @Test
    public void update_UserImportScope_returnsOverwrittenDTO() throws ParseException {

        // given a record
        Record record = TestHelper.mockRecord();
        record.setStatus(Record.Status.DRAFT);
        record.setCreatedAt(new Date());
        record = repository.saveAndFlush(record);

        // given the user with proper permission
        User user = TestHelper.mockUser();
        TestHelper.addResourceAndScopePermissionToUser(
                user, record.getAllocationID().toString(),
                Sets.newHashSet(Scope.UPDATE.getValue(), Scope.IMPORT.getValue())
        );

        // the update payload contains new createdAt, modifiedAt and creatorID
        Date updatedCreatedAt = new SimpleDateFormat("dd/MM/yyyy").parse("02/02/1989");
        Date updatedModifiedAt = new SimpleDateFormat("dd/MM/yyyy").parse("03/03/1989");
        UUID updatedCreatorID = UUID.randomUUID();
        RecordDTO dto = mapper.convertToDTO(record);
        dto.setModifiedAt(updatedModifiedAt);
        dto.setCreatedAt(updatedCreatedAt);
        dto.setCreatorID(updatedCreatorID);

        // when update
        RecordDTO resultDTO = service.update(dto, user);

        // the resultDTO contains the updated fields with no exception
        assertThat(resultDTO).isNotNull();
        assertThat(resultDTO.getCreatedAt()).isEqualTo(updatedCreatedAt);
        assertThat(resultDTO.getModifiedAt()).isEqualTo(updatedModifiedAt);
        assertThat(resultDTO.getCreatorID()).isEqualTo(updatedCreatorID);

        // and the result record in the database contains the updated fields
        Record actualRecord = service.findById(record.getId().toString());
        assertThat(actualRecord.getCreatedAt()).isEqualTo(updatedCreatedAt);
        assertThat(actualRecord.getModifiedAt()).isEqualTo(updatedModifiedAt);
        assertThat(actualRecord.getCreatorID()).isEqualTo(updatedCreatorID);
    }

    @Test
    public void exists_recordExists_returnsTrue() {
        // random uuid doesn't exist
        assertThat(service.exists(UUID.randomUUID().toString())).isFalse();

        // when a record is created
        Record record = repository.save(new Record());

        // it exists
        assertThat(service.exists(record.getId().toString())).isTrue();
    }

    @Test
    public void delete_UserSufficientPermission_returnsTrue() {
        // given an allocation
        UUID allocationID = UUID.randomUUID();

        // given a user with update scope to that allocation
        User user = TestHelper.mockUser();
        TestHelper.addResourceAndScopePermissionToUser(user, allocationID.toString(), Sets.newHashSet(Scope.UPDATE.getValue()));

        // given a record that has that allocation
        Record record = new Record();
        record.setCreatedAt(new Date());
        record.setAllocationID(allocationID);
        record = repository.save(record);

        // (sanity check) that record exists
        assertThat(service.exists(record.getId().toString())).isTrue();

        // when delete
        boolean result = service.delete(record.getId().toString(), user);

        // it returns true and it's gone
        assertThat(result).isTrue();
        assertThat(service.exists(record.getId().toString())).isFalse();
    }

}
