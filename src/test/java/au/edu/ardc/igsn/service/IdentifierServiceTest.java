package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.TestHelper;
import au.edu.ardc.igsn.entity.Identifier;
import au.edu.ardc.igsn.repository.IdentifierRepository;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class IdentifierServiceTest {

    @Autowired
    private IdentifierService service;

    @MockBean
    private IdentifierRepository repository;

    @Test
    public void it_can_find_identifier_by_id() {
        UUID id = UUID.randomUUID();
        Identifier identifier = TestHelper.mockIdentifier(id);
        when(repository.findById(id)).thenReturn(Optional.of(identifier));

        Identifier actual = service.findById(id.toString());

        // ensure repository call findById
        verify(repository, times(1)).findById(any(UUID.class));

        assertThat(actual).isInstanceOf(Identifier.class);
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
        Identifier newIdentifier = TestHelper.mockIdentifier();
        service.create(newIdentifier);
        verify(repository, times(1)).save(newIdentifier);
    }

    @Test
    public void it_updates_identifier_correctly() {
        String new_value = "changed_Value";
        Identifier actual = TestHelper.mockIdentifier();
        String update_id = actual.getId().toString();
        String original_value = actual.getValue();
        when(repository.save(any(Identifier.class))).thenReturn(actual);
        actual.setValue(new_value);
        Identifier updated = service.update(actual);

        // the save method is invoked on the repository
        verify(repository, times(1)).save(any(Identifier.class));
        String compare_id = updated.getId().toString();
        String updated_value = updated.getValue();

        // the updated identifier is returned with updated values
        Assertions.assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(actual.getUpdatedAt());
        Assertions.assertThat(update_id.equals(compare_id));
        Assertions.assertThat(new_value.equals(updated_value));
    }
}
