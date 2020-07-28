package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.TestHelper;
import au.edu.ardc.igsn.dto.IdentifierDTO;
import au.edu.ardc.igsn.dto.IdentifierMapper;
import au.edu.ardc.igsn.dto.VersionDTO;
import au.edu.ardc.igsn.entity.Identifier;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.entity.Version;
import au.edu.ardc.igsn.exception.ForbiddenOperationException;
import au.edu.ardc.igsn.exception.RecordNotFoundException;
import au.edu.ardc.igsn.model.User;
import au.edu.ardc.igsn.repository.IdentifierRepository;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {IdentifierService.class, IdentifierMapper.class, ModelMapper.class})
public class IdentifierServiceTest {

    @MockBean
    ValidationService validationService;

    @Autowired
    private IdentifierService service;

    @MockBean
    private RecordService recordService;

    @MockBean
    private IdentifierRepository repository;

    @Test
    public void findById_IdentifierFound_returnsIdentifier() {
        UUID id = UUID.randomUUID();
        Identifier identifier = TestHelper.mockIdentifier(id);
        when(repository.findById(id)).thenReturn(Optional.of(identifier));

        Identifier actual = service.findById(id.toString());

        // ensure repository call findById
        verify(repository, times(1)).findById(any(UUID.class));
        assertThat(actual).isNotNull();
        assertThat(actual).isInstanceOf(Identifier.class);
    }

    @Test
    public void findById_IdentifierNotFound_returnsNull() {
        Identifier actual = service.findById(UUID.randomUUID().toString());

        // ensure repository call findById
        verify(repository, times(1)).findById(any(UUID.class));
        assertThat(actual).isNull();
    }

    @Test
    public void exists_callsExistsByID_returnsTrue() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(true);

        assertThat(service.exists(id.toString())).isTrue();

        // ensure repository call existsById
        verify(repository, times(1)).existsById(any(UUID.class));

        // false case
        assertThat(service.exists(UUID.randomUUID().toString())).isFalse();
    }

    @Test
    public void create_RecordNotFound_throwsException() {
        // given a version dto
        IdentifierDTO dto = new IdentifierDTO();
        dto.setRecord(UUID.randomUUID());

        when(recordService.exists(dto.getRecord().toString())).thenReturn(false);

        // when the service creates the version, expects exception
        Assert.assertThrows(RecordNotFoundException.class, () -> {
            service.create(dto, TestHelper.mockUser());
        });
    }

    @Test
    void create_failValidateRecordOwnership_throwsException() {
        // given a user & record that doesn't belong to that user
        User user = TestHelper.mockUser();
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        record.setOwnerType(Record.OwnerType.User);
        record.setOwnerID(UUID.randomUUID());

        // given a version dto for that record
        IdentifierDTO dto = new IdentifierDTO();
        dto.setRecord(record.getId());

        // setup the world
        when(recordService.exists(record.getId().toString())).thenReturn(true);
        when(validationService.validateRecordOwnership(any(Record.class), any(User.class))).thenReturn(false);

        Assert.assertThrows(ForbiddenOperationException.class, () -> {
            service.create(dto, user);
        });
    }

    @Test
    public void create_ValidRequest_returnsDTO() {
        // given a record & user
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        User user = TestHelper.mockUser();

        // given a version dto
        IdentifierDTO dto = new IdentifierDTO();
        dto.setRecord(record.getId());
        dto.setType(Identifier.Type.IGSN);
        dto.setValue("IGSNVALUE");

        // setup repository mock
        Identifier expected = TestHelper.mockIdentifier(record);
        when(recordService.exists(anyString())).thenReturn(true);
        when(recordService.findById(anyString())).thenReturn(record);
        when(validationService.validateRecordOwnership(any(Record.class), any(User.class))).thenReturn(true);
        when(repository.save(any(Identifier.class))).thenReturn(expected);

        // when the service creates the version, verify the save method is called
        IdentifierDTO resultDTO = service.create(dto, user);
        assertThat(resultDTO).isNotNull();
        assertThat(resultDTO).isInstanceOf(IdentifierDTO.class);
        verify(repository, times(1)).save(any(Identifier.class));
    }

    @Test
    public void it_can_delete_version_by_id() {
        UUID id = UUID.randomUUID();

        service.delete(id.toString());
        // ensure repository call deleteById
        verify(repository, times(1)).deleteById(any(String.class));
    }

    @Test
    public void it_can_create_a_version() {
        Identifier newIdentifier = TestHelper.mockIdentifier();
        service.create(newIdentifier);
        verify(repository, times(1)).save(newIdentifier);
    }
}
