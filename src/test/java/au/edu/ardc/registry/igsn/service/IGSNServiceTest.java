package au.edu.ardc.registry.igsn.service;

import au.edu.ardc.registry.TestHelper;
import au.edu.ardc.registry.igsn.config.IGSNProperties;
import au.edu.ardc.registry.igsn.entity.IGSNServiceRequest;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.repository.IGSNServiceRequestRepository;
import au.edu.ardc.registry.igsn.service.IGSNService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {IGSNService.class})
class IGSNServiceTest {

    @MockBean
    private IGSNServiceRequestRepository repository;

    @MockBean
    private IGSNProperties properties;

    @Autowired
    private IGSNService service;

    @Test
    void findById_foundRecord_returnsIGSNServiceRequest() {
        IGSNServiceRequest request = new IGSNServiceRequest();
        when(repository.findById(any(UUID.class))).thenReturn(Optional.of(request));

        IGSNServiceRequest actual = service.findById(UUID.randomUUID().toString());

        verify(repository, times(1)).findById(any(UUID.class));
        assertThat(actual).isNotNull();
    }

    @Test
    void findById_notfound_returnsNull() {
        IGSNServiceRequest actual = service.findById(UUID.randomUUID().toString());

        verify(repository, times(1)).findById(any(UUID.class));
        assertThat(actual).isNull();
    }

    @Test
    void createRequest() {
        // todo pull data path from src/test/resources/application.properties instead of mocking

        String randomDataPath = "/tmp/" + UUID.randomUUID().toString();
        User user = TestHelper.mockUser();
        IGSNServiceRequest request = new IGSNServiceRequest();
        request.setId(UUID.randomUUID());
        request.setDataPath(randomDataPath + "/" + request.getId());

        when(repository.save(any(IGSNServiceRequest.class))).thenReturn(request);
        when(properties.getDataPath()).thenReturn(randomDataPath);

        IGSNServiceRequest actual = service.createRequest(user);

        verify(repository, times(2)).save(any(IGSNServiceRequest.class));

        // ensure directory path is created
        assertThat(actual).isNotNull();
        assertThat(request.getDataPath()).isNotNull();
        assertThat(new File(randomDataPath).exists()).isTrue();
        assertThat(new File(randomDataPath).canRead()).isTrue();
        assertThat(new File(randomDataPath).canWrite()).isTrue();
        assertThat(new File(request.getDataPath()).exists()).isTrue();
        assertThat(new File(request.getDataPath()).canRead()).isTrue();
        assertThat(new File(request.getDataPath()).canWrite()).isTrue();

        // clean up
        new File(randomDataPath).deleteOnExit();
    }
}