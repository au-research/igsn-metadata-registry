package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.Scope;
import au.edu.ardc.igsn.TestHelper;
import au.edu.ardc.igsn.User;
import au.edu.ardc.igsn.dto.RecordDTO;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.exception.ForbiddenOperationException;
import au.edu.ardc.igsn.exception.RecordNotFoundException;
import au.edu.ardc.igsn.repository.RecordRepository;
import com.google.common.collect.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

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
    public void findById_recordExists_returnRecord() {
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

    @Test(expected=RecordNotFoundException.class)
    public void findById_recordDoesNotExist_ExceptionRecordNotFound(){
        // given a record & a user
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        User user = TestHelper.mockUser();

        // mock repository
        when(repository.findById(record.getId())).thenReturn(Optional.empty());

        // when findById expects RecordNotFoundException
        service.findById(record.getId().toString(), user);
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

    @Test(expected = ForbiddenOperationException.class)
    public void validate_UserDoesNotHaveAccessToAnyAllocation_throwsException() {
        // given a User & Record
        User user = TestHelper.mockUser();
        Record record = TestHelper.mockRecord();

        // when validate expects ForbiddenOperationException
        service.validate(record, user);
    }

    @Test(expected = ForbiddenOperationException.class)
    public void validate_UserDoesNotHaveAccessToAllocation_throwsException() {
        // given a User & Record
        User user = TestHelper.mockUser();
        TestHelper.addResourceAndScopePermissionToUser(user, UUID.randomUUID().toString(), Sets.newHashSet(Scope.CREATE.getValue()));
        Record record = TestHelper.mockRecord();

        // when validate expects ForbiddenOperationException
        service.validate(record, user);
    }

    @Test
    public void validate_UserHaveAccessToRightAllocation_returnsTrue() {
        // given a User & Record
        User user = TestHelper.mockUser();
        UUID resourceID = UUID.randomUUID();
        TestHelper.addResourceAndScopePermissionToUser(user, resourceID.toString(), Sets.newHashSet(Scope.CREATE.getValue()));
        Record record = TestHelper.mockRecord();
        record.setAllocationID(resourceID);

        // when validate
        boolean actual = service.validate(record, user);

        assertThat(actual).isTrue();
    }

    @Test(expected = ForbiddenOperationException.class)
    public void validate_UserDoesNotHaveAccessToScope_throwsException() {
        // given a User & Record
        User user = TestHelper.mockUser();
        UUID resourceID = UUID.randomUUID();
        TestHelper.addResourceAndScopePermissionToUser(user, resourceID.toString(), Sets.newHashSet(Scope.CREATE.getValue()));
        Record record = TestHelper.mockRecord();
        record.setAllocationID(resourceID);

        // when validate expects ForbiddenOperationException
        service.validate(record, user, Scope.IMPORT);
    }

    @Test
    public void validate_UserHaveAccessToRightScope_returnsTrue() {
        // given a User & Record
        User user = TestHelper.mockUser();
        UUID resourceID = UUID.randomUUID();
        TestHelper.addResourceAndScopePermissionToUser(user, resourceID.toString(), Sets.newHashSet(Scope.CREATE.getValue()));
        Record record = TestHelper.mockRecord();
        record.setAllocationID(resourceID);

        // when validate
        boolean actual =  service.validate(record, user, Scope.CREATE);

        assertThat(actual).isTrue();
    }

    @Test(expected = ForbiddenOperationException.class)
    public void create_UserDoesNotHaveAccessToAllocation_throwsException() {
        // given a User with the wrong permission
        User user = TestHelper.mockUser();
        UUID resourceID = UUID.randomUUID();
        TestHelper.addResourceAndScopePermissionToUser(user, UUID.randomUUID().toString(), Sets.newHashSet(Scope.CREATE.getValue()));

        // given a dto
        RecordDTO dto = new RecordDTO();
        dto.setAllocationID(resourceID);

        // expects ForbiddenOperationException
        service.create(dto, user);
    }

    @Test(expected = ForbiddenOperationException.class)
    public void create_UserDoesNotHaveScope_throwsException() {
        // given a User with the wrong permission
        User user = TestHelper.mockUser();
        UUID resourceID = UUID.randomUUID();
        TestHelper.addResourceAndScopePermissionToUser(user, resourceID.toString(), Sets.newHashSet(Scope.IMPORT.getValue()));

        // given a dto
        RecordDTO dto = new RecordDTO();
        dto.setAllocationID(resourceID);

        // expects ForbiddenOperationException
        service.create(dto, user);
    }

    @Test
    public void create_UserHasSufficientPermission_returnsDTO() {
        // given a User with the wrong permission
        User user = TestHelper.mockUser();
        UUID resourceID = UUID.randomUUID();
        TestHelper.addResourceAndScopePermissionToUser(user, resourceID.toString(), Sets.newHashSet(Scope.CREATE.getValue()));

        // given a dto
        RecordDTO dto = new RecordDTO();
        dto.setAllocationID(resourceID);

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

    // todo delete_UserDoesNotHaveAllocation_throwsException

    @Test(expected = ForbiddenOperationException.class)
    public void delete_UserDoesNotHaveRightScope_throwsException() {
        // given a User with the wrong permission
        User user = TestHelper.mockUser();
        UUID resourceID = UUID.randomUUID();
        TestHelper.addResourceAndScopePermissionToUser(user, resourceID.toString(), Sets.newHashSet(Scope.IMPORT.getValue()));

        // given a record
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        record.setAllocationID(resourceID);

        // record exists for deletion
        when(repository.existsById(record.getId())).thenReturn(true);
        when(repository.findById(record.getId())).thenReturn(Optional.of(record));

        // expects ForbiddenOperationException
        service.delete(record.getId().toString(), user);
    }

    @Test
    public void delete_UserSufficientPermission_returnsTrue() {
        // given a User with the wrong permission
        User user = TestHelper.mockUser();
        UUID resourceID = UUID.randomUUID();
        TestHelper.addResourceAndScopePermissionToUser(user, resourceID.toString(), Sets.newHashSet(Scope.UPDATE.getValue()));

        // given a record with that allocation
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        record.setAllocationID(resourceID);

        // record exists for deletion
        when(repository.existsById(record.getId())).thenReturn(true);
        when(repository.findById(record.getId())).thenReturn(Optional.of(record));

        // when delete
        boolean result = service.delete(record.getId().toString(), user);

        assertThat(result).isTrue();
        verify(repository, times(1)).delete(any(Record.class));
    }

    @Test(expected = RecordNotFoundException.class)
    public void update_RecordDoesNotExist_throwsException() {
        // given a random dto
        RecordDTO dto = new RecordDTO();
        dto.setId(UUID.randomUUID());

        // when update with a random user expects RecordNotFoundException
        service.update(dto, TestHelper.mockUser());
    }

    @Test(expected = ForbiddenOperationException.class)
    public void update_UserDoesNotHaveAllocation_throwsException() {
        // given a User with the wrong permission
        User user = TestHelper.mockUser();
        UUID resourceID = UUID.randomUUID();
        TestHelper.addResourceAndScopePermissionToUser(user, UUID.randomUUID().toString(), Sets.newHashSet(Scope.CREATE.getValue()));

        // an existing record
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        record.setAllocationID(resourceID);

        // given a dto
        RecordDTO dto = new RecordDTO();
        dto.setId(record.getId());

        // pass the existence test
        when(repository.existsById(dto.getId())).thenReturn(true);
        when(repository.findById(any(UUID.class))).thenReturn(Optional.of(record));

        // expects ForbiddenOperationException
        service.update(dto, user);
    }

    @Test(expected = ForbiddenOperationException.class)
    public void update_UserDoesNotHaveRightScope_throwsException() {
        // given a User with the wrong permission
        User user = TestHelper.mockUser();
        UUID resourceID = UUID.randomUUID();
        TestHelper.addResourceAndScopePermissionToUser(user, resourceID.toString(), Sets.newHashSet(Scope.IMPORT.getValue()));

        // an existing record
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        record.setAllocationID(resourceID);

        // given a dto
        RecordDTO dto = new RecordDTO();
        dto.setId(record.getId());

        // pass the exist test
        when(repository.existsById(dto.getId())).thenReturn(true);
        when(repository.findById(any(UUID.class))).thenReturn(Optional.of(record));

        // expects ForbiddenOperationException
        service.update(dto, user);
    }

    @Test
    public void update_UserSufficientPermission_returnsDTO() {
        // given a User with the wrong permission
        User user = TestHelper.mockUser();
        UUID resourceID = UUID.randomUUID();
        TestHelper.addResourceAndScopePermissionToUser(user, resourceID.toString(), Sets.newHashSet(Scope.UPDATE.getValue()));

        // an existing record
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        record.setAllocationID(resourceID);

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
        // given a User with the wrong permission
        User user = TestHelper.mockUser();
        UUID resourceID = UUID.randomUUID();
        TestHelper.addResourceAndScopePermissionToUser(
                user,
                resourceID.toString(),
                Sets.newHashSet(Scope.IMPORT.getValue(), Scope.UPDATE.getValue())
        );

        // given existing record
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        record.setAllocationID(resourceID);

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