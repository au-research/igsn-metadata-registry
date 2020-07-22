package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.TestHelper;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import au.edu.ardc.igsn.entity.URL;
import au.edu.ardc.igsn.repository.URLRepository;
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
public class URLServiceTest {

    @Autowired
    private URLService service;

    @MockBean
    private URLRepository repository;

    @Test
    public void it_can_find_url_by_id() {
        UUID id = UUID.randomUUID();
        URL url = TestHelper.mockUrl(id);
        when(repository.findById(id)).thenReturn(Optional.of(url));

        URL actual = service.findById(id.toString());

        // ensure repository call findById
        verify(repository, times(1)).findById(any(UUID.class));

        assertThat(actual).isInstanceOf(URL.class);
    }

    @Test
    public void it_can_find_url_existence_by_id() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(true);

        assertThat(service.exists(id.toString())).isTrue();

        // ensure repository call findById
        verify(repository, times(1)).existsById(any(UUID.class));

        // false case
        assertThat(service.exists(UUID.randomUUID().toString())).isFalse();
    }

    @Test
    public void it_can_delete_url_by_id() {
        UUID id = UUID.randomUUID();

        service.delete(id.toString());
        // ensure repository call deleteById
        verify(repository, times(1)).deleteById(any(String.class));
    }

    @Test
    @Transactional
    public void it_can_create_a_url() {
        URL newUrl = TestHelper.mockUrl();
        service.create(newUrl);
        verify(repository, times(1)).save(newUrl);
    }

    @Test
    public void it_updates_url_correctly() {
        String new_url = "http://changed_url.com";
        URL actual = TestHelper.mockUrl();
        String update_id = actual.getId().toString();

        when(repository.save(any(URL.class))).thenReturn(actual);
        actual.setUrl(new_url);
        URL updated = service.update(actual);

        // the save method is invoked on the repository
        verify(repository, times(1)).save(any(URL.class));
        String compare_id = updated.getId().toString();
        String updated_url = updated.getUrl();

        // the updated url is returned with updated values
        Assertions.assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(actual.getUpdatedAt());
        Assertions.assertThat(update_id.equals(compare_id));
        Assertions.assertThat(new_url.equals(updated_url));
    }
}
