package au.edu.ardc.igsn.service;


import au.edu.ardc.igsn.TestHelper;
import au.edu.ardc.igsn.dto.RecordDTO;
import au.edu.ardc.igsn.dto.RecordMapper;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.exception.ForbiddenOperationException;
import au.edu.ardc.igsn.exception.RecordNotFoundException;
import au.edu.ardc.igsn.model.Allocation;
import au.edu.ardc.igsn.model.DataCenter;
import au.edu.ardc.igsn.model.Scope;
import au.edu.ardc.igsn.model.User;
import au.edu.ardc.igsn.repository.RecordRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RecordService.class, RecordMapper.class, ModelMapper.class, ValidationService.class})
public class RecordServiceTest {

    @Autowired
    private RecordService service;

    @MockBean
    private RecordRepository repository;

    @Test
    void findById_recordExists_returnRecord() {
        // given a record & user
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        User user = TestHelper.mockUser();

        // mock repository
        when(repository.findById(record.getId())).thenReturn(Optional.of(record));

        // when findById
        RecordDTO actual = service.findById(record.getId().toString(), user);

        // returns the same record
        assertThat(actual).isNotNull();
        assertThat(actual).isInstanceOf(RecordDTO.class);
        assertThat(actual.getId()).isEqualTo(record.getId());
    }

    @Test
    public void findById_recordDoesNotExist_ExceptionRecordNotFound(){
        // given a record & a user
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        User user = TestHelper.mockUser();

        // mock repository
        when(repository.findById(record.getId())).thenReturn(Optional.empty());

        // when findById expects RecordNotFoundException
        Assertions.assertThrows(RecordNotFoundException.class, () -> {
            service.findById(record.getId().toString(), user);
        });
    }

    @Test
    public void exists_recordExist_returnTrue() {
        UUID randomUUID = UUID.randomUUID();
        when(repository.existsById(randomUUID)).thenReturn(true);
        assertThat(service.exists(randomUUID.toString())).isTrue();
    }

    @Test
    public void exists_recordDoesNotExist_returnFalse() {
        UUID randomUUID = UUID.randomUUID();
        when(repository.existsById(randomUUID)).thenReturn(false);
        assertThat(service.exists(randomUUID.toString())).isFalse();
    }

    @Test
    public void create_UserDoesNotHaveAccessToAllocation_throwsException() {
        // given a User with the wrong permission
        User user = TestHelper.mockUser();

        // given a dto
        RecordDTO dto = new RecordDTO();

        // expects ForbiddenOperationException
        Assertions.assertThrows(ForbiddenOperationException.class, () -> {
            service.create(dto, user);
        });
    }

    @Test
    public void create_UserDoesNotHaveScope_throwsException() {
        // given a User with the wrong scope
        User user = TestHelper.mockUser();
        Allocation allocation = new Allocation(UUID.randomUUID());
        allocation.setScopes(Arrays.asList(Scope.UPDATE));
        user.setPermissions(Arrays.asList(allocation));

        // given a dto
        RecordDTO dto = new RecordDTO();
        dto.setAllocationID(allocation.getId());

        // expects ForbiddenOperationException
        Assertions.assertThrows(ForbiddenOperationException.class, () -> {
            service.create(dto, user);
        });
    }

    @Test
    public void create_UserHasSufficientPermission_returnsDTO() {
        // given a User with the right permission
        User user = TestHelper.mockUser();
        Allocation allocation = new Allocation(UUID.randomUUID());
        allocation.setScopes(Arrays.asList(Scope.CREATE));
        user.setPermissions(Arrays.asList(allocation));

        // given a dto
        RecordDTO dto = new RecordDTO();
        dto.setAllocationID(allocation.getId());

        // setup repository mock
        Record expected = TestHelper.mockRecord(UUID.randomUUID());
        when(repository.save(any(Record.class))).thenReturn(expected);

        // when the service creates the record with the dto and the user
        RecordDTO result = service.create(dto, user);

        // dto exists and repository.save is called
        assertThat(result).isInstanceOf(RecordDTO.class);
        assertThat(result.getId()).isNotNull();
        verify(repository, times(1)).save(any(Record.class));
    }

    @Test
    public void delete_NotOwned_throwsException() {
        // given a User with the wrong permission
        User user = TestHelper.mockUser();

        // given a record
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        record.setOwnerType(Record.OwnerType.DataCenter);
        record.setOwnerID(UUID.randomUUID());

        // record exists for deletion
        when(repository.existsById(record.getId())).thenReturn(true);
        when(repository.findById(record.getId())).thenReturn(Optional.of(record));

        // expects ForbiddenOperationException
        Assertions.assertThrows(ForbiddenOperationException.class, () -> {
            service.delete(record.getId().toString(), user);
        });
    }

    @Test
    public void delete_UserSufficientPermission_returnsTrue() {
        // given a User with the wrong permission
        User user = TestHelper.mockUser();

        // given a record with that allocation
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        record.setOwnerID(user.getId());
        record.setOwnerType(Record.OwnerType.User);

        // record exists for deletion
        when(repository.existsById(record.getId())).thenReturn(true);
        when(repository.findById(record.getId())).thenReturn(Optional.of(record));

        // when delete
        boolean result = service.delete(record.getId().toString(), user);

        assertThat(result).isTrue();
        verify(repository, times(1)).delete(any(Record.class));
    }

    @Test
    public void update_RecordDoesNotExist_throwsException() {
        // given a random dto
        RecordDTO dto = new RecordDTO();
        dto.setId(UUID.randomUUID());

        // when update with a random user expects RecordNotFoundException
        Assertions.assertThrows(RecordNotFoundException.class, () -> {
            service.update(dto, TestHelper.mockUser());
        });
    }

    @Test
    public void update_OwnerTypeUserUserDoesNotHavePermission_throwsException() {
        // an existing record
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        record.setOwnerType(Record.OwnerType.User);
        record.setOwnerID(UUID.randomUUID());

        // given a User with the wrong permission
        User user = TestHelper.mockUser();

        // given a dto
        RecordDTO dto = new RecordDTO();
        dto.setId(record.getId());

        // pass the existence test
        when(repository.existsById(dto.getId())).thenReturn(true);
        when(repository.findById(any(UUID.class))).thenReturn(Optional.of(record));

        // expects ForbiddenOperationException
        Assertions.assertThrows(ForbiddenOperationException.class, () -> {
            service.update(dto, user);
        });
    }

    @Test
    public void update_OnwerTypeDataCenter_throwsException() {
        // an existing record
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        record.setOwnerType(Record.OwnerType.DataCenter);
        record.setOwnerID(UUID.randomUUID());

        // given a User with the wrong permission
        User user = TestHelper.mockUser();

        // given a dto
        RecordDTO dto = new RecordDTO();
        dto.setId(record.getId());

        // pass the exist test
        when(repository.existsById(dto.getId())).thenReturn(true);
        when(repository.findById(any(UUID.class))).thenReturn(Optional.of(record));

        // expects ForbiddenOperationException
        Assertions.assertThrows(ForbiddenOperationException.class, () -> {
            service.update(dto, user);
        });
    }

    @Test
    public void update_OnwerTypeUser_returnsDTO() {
        // given a User
        User user = TestHelper.mockUser();

        // owns an existing record
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        record.setOwnerType(Record.OwnerType.User);
        record.setOwnerID(user.getId());

        // given a dto
        RecordDTO dto = new RecordDTO();
        dto.setId(record.getId());

        // make sure when repository.save call returns a mockRecord and the record exists
        when(repository.existsById(dto.getId())).thenReturn(true);
        when(repository.findById(any(UUID.class))).thenReturn(Optional.of(record));
        when(repository.save(any(Record.class))).thenReturn(record);

        RecordDTO expected = service.update(dto, user);
        assertThat(expected).isInstanceOf(RecordDTO.class);
        verify(repository, times(1)).save(any(Record.class));
    }

    @Test
    public void update_OnwerTypeDataCenter_returnsDTO() {
        // datacenter
        DataCenter dataCenter = new DataCenter(UUID.randomUUID());

        // given a User
        User user = TestHelper.mockUser();
        user.setDataCenters(Arrays.asList(dataCenter));

        // owns an existing record
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        record.setOwnerType(Record.OwnerType.DataCenter);
        record.setOwnerID(dataCenter.getId());
        record.setDataCenterID(dataCenter.getId());

        // given a dto
        RecordDTO dto = new RecordDTO();
        dto.setId(record.getId());

        // make sure when repository.save call returns a mockRecord and the record exists
        when(repository.existsById(dto.getId())).thenReturn(true);
        when(repository.findById(any(UUID.class))).thenReturn(Optional.of(record));
        when(repository.save(any(Record.class))).thenReturn(record);

        RecordDTO expected = service.update(dto, user);
        assertThat(expected).isInstanceOf(RecordDTO.class);
        verify(repository, times(1)).save(any(Record.class));
    }

    @Test
    public void update_UserImportScope_returnsDTO() throws ParseException {
        // given a User with the import scope to the record
        User user = TestHelper.mockUser();
        Allocation allocation = new Allocation(UUID.randomUUID());
        allocation.setScopes(Arrays.asList(Scope.UPDATE, Scope.IMPORT));

        // given existing record
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        record.setOwnerType(Record.OwnerType.User);
        record.setOwnerID(user.getId());
        record.setAllocationID(allocation.getId());

        // given a dto
        RecordDTO dto = new RecordDTO();
        dto.setId(record.getId());
        dto.setCreatedAt(new SimpleDateFormat("dd/MM/yyyy").parse("02/02/1989"));
        dto.setModifiedAt(new SimpleDateFormat("dd/MM/yyyy").parse("02/02/1989"));
        dto.setCreatorID(UUID.randomUUID());

        // make sure when repository.save call returns a mockRecord and the record exists
        when(repository.existsById(dto.getId())).thenReturn(true);
        when(repository.findById(dto.getId())).thenReturn(Optional.of(record));
        when(repository.save(any(Record.class))).thenReturn(TestHelper.mockRecord());

        RecordDTO expected = service.update(dto, user);
        assertThat(expected).isInstanceOf(RecordDTO.class);
        verify(repository, times(1)).save(any(Record.class));
    }

}