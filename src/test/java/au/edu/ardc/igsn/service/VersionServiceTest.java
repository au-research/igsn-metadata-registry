package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.TestHelper;
import au.edu.ardc.igsn.entity.Version;
import au.edu.ardc.igsn.repository.VersionRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class VersionServiceTest {
    @Autowired
    private VersionService service;

    @MockBean
    private VersionRepository repository;


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

    @Test
    public void it_can_delete_version_by_id() {
        UUID id = UUID.randomUUID();

        service.delete(id.toString());
        // ensure repository call deleteById
        verify(repository, times(1)).deleteById(any(String.class));
    }

    @Test
    @Transactional
    public void it_can_create_a_version() {
        Version newVersion = TestHelper.mockVersion();
        service.create(newVersion);
        verify(repository, times(1)).save(newVersion);
    }
}