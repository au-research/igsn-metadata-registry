package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.TestHelper;
import au.edu.ardc.igsn.dto.VersionDTO;
import au.edu.ardc.igsn.dto.VersionMapper;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.entity.Version;
import au.edu.ardc.igsn.exception.ForbiddenOperationException;
import au.edu.ardc.igsn.exception.RecordNotFoundException;
import au.edu.ardc.igsn.exception.SchemaNotSupportedException;
import au.edu.ardc.igsn.exception.VersionNotFoundException;
import au.edu.ardc.igsn.model.User;
import au.edu.ardc.igsn.repository.VersionRepository;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        VersionService.class, SchemaService.class,
        VersionMapper.class, ModelMapper.class
})
public class VersionServiceTest {

    @Autowired
    VersionService service;

    @MockBean
    RecordService recordService;

    @MockBean
    VersionRepository repository;

    @MockBean
    ValidationService validationService;

    @Test
    public void create_ValidRequest_returnsDTO() {
        // given a record & user
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        User user = TestHelper.mockUser();

        // given a version dto
        VersionDTO dto = new VersionDTO();
        dto.setRecord(record.getId().toString());
        dto.setSchema("igsn-descriptive-v1");
        dto.setContent("blah");

        // setup repository mock
        Version expected = TestHelper.mockVersion(record.getId());
        when(recordService.exists(anyString())).thenReturn(true);
        when(recordService.findById(anyString())).thenReturn(record);
        when(validationService.validateRecordOwnership(any(Record.class), any(User.class))).thenReturn(true);
        when(repository.save(any(Version.class))).thenReturn(expected);

        // when the service creates the version, verify the save method is called
        VersionDTO resultDTO = service.create(dto, user);
        assertThat(resultDTO).isNotNull();
        assertThat(resultDTO).isInstanceOf(VersionDTO.class);
        verify(repository, times(1)).save(any(Version.class));
    }

    @Test
    public void create_InvalidSchema_throwsException() {
        // given a record & user
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        User user = TestHelper.mockUser();

        // given a version dto
        VersionDTO dto = new VersionDTO();
        dto.setRecord(record.getId().toString());
        dto.setSchema("not-supported");
        dto.setContent("blah");

        // when the service creates the version, expects exception
        Assert.assertThrows(SchemaNotSupportedException.class, () -> {
            service.create(dto, user);
        });
    }

    @Test
    public void create_RecordNotFound_throwsException() {
        // given a version dto
        VersionDTO dto = new VersionDTO();
        dto.setRecord(UUID.randomUUID().toString());
        dto.setSchema("igsn-descriptive-v1");
        dto.setContent("blah");

        when(recordService.exists(dto.getId())).thenReturn(false);

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
        VersionDTO dto = new VersionDTO();
        dto.setRecord(record.getId().toString());
        dto.setSchema("igsn-descriptive-v1");
        dto.setContent("blah");

        // setup the world
        when(recordService.exists(record.getId().toString())).thenReturn(true);
        when(validationService.validateRecordOwnership(any(Record.class), any(User.class))).thenReturn(false);

        Assert.assertThrows(ForbiddenOperationException.class, () -> {
            service.create(dto, user);
        });
    }

    @Test
    void delete_VersionNotFound_throwException() {
        Assert.assertThrows(VersionNotFoundException.class, () -> {
            service.delete(UUID.randomUUID().toString(), TestHelper.mockUser());
        });
    }

    @Test
    void delete_VersionNotOwned_throwsException() {
        // given a record + version
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        record.setOwnerType(Record.OwnerType.User);
        record.setOwnerID(UUID.randomUUID());
        Version version = TestHelper.mockVersion(record.getId());

        // mock repository
        when(repository.existsById(version.getId())).thenReturn(true);
        when(repository.findById(version.getId())).thenReturn(Optional.of(version));
        when(validationService.validateRecordOwnership(any(Record.class), any(User.class))).thenReturn(false);

        // throws ForbiddenOperationException
        Assert.assertThrows(ForbiddenOperationException.class, () -> {
            service.delete(version.getId().toString(), TestHelper.mockUser());
        });
    }

    @Test
    void delete_ValidRequest_returnsTrue() {
        // given a record + version
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        record.setOwnerType(Record.OwnerType.User);
        record.setOwnerID(UUID.randomUUID());
        Version version = TestHelper.mockVersion(record.getId());

        // mock repository
        when(repository.existsById(version.getId())).thenReturn(true);
        when(repository.findById(version.getId())).thenReturn(Optional.of(version));
        when(validationService.validateRecordOwnership(any(Record.class), any(User.class))).thenReturn(true);

        // when delete, repository.deleteById is called
        service.delete(version.getId().toString(), TestHelper.mockUser());
        verify(repository, times(1)).deleteById(version.getId().toString());
    }

    // todo update
    // todo end
    // todo findById
    // todo findOwned

    @Test
    public void it_can_end_the_life_of_a_version() {
        Version version = TestHelper.mockVersion();
        when(repository.save(any(Version.class))).thenReturn(version);

        Version endedVersion = service.end(version);

        // ensure the repository call save
        verify(repository, times(1)).save(version);

        assertThat(endedVersion.getStatus()).isEqualTo(Version.Status.SUPERSEDED);
        assertThat(endedVersion.getEndedAt()).isNotNull();
        assertThat(endedVersion.getEndedAt()).isInstanceOf(Date.class);
    }

    @Test
    public void it_can_find_version_by_id() {
        UUID id = UUID.randomUUID();
        Version version = TestHelper.mockVersion(id);
        when(repository.findById(id)).thenReturn(Optional.of(version));

        Version actual = service.findById(id.toString());

        // ensure repository call findById
        verify(repository, times(1)).findById(any(UUID.class));

        assertThat(actual).isInstanceOf(Version.class);
    }

    @Test
    public void it_can_find_version_existence_by_id() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(true);

        assertThat(service.exists(id.toString())).isTrue();

        // ensure repository call findById
        verify(repository, times(1)).existsById(any(UUID.class));

        // false case
        assertThat(service.exists(UUID.randomUUID().toString())).isFalse();
    }
}