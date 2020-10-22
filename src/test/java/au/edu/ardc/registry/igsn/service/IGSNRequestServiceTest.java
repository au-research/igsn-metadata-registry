package au.edu.ardc.registry.igsn.service;

import au.edu.ardc.registry.TestHelper;
import au.edu.ardc.registry.common.config.ApplicationProperties;
import au.edu.ardc.registry.common.dto.mapper.RequestMapper;
import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.model.Attribute;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.repository.RequestRepository;
import au.edu.ardc.registry.common.service.RequestService;
import au.edu.ardc.registry.igsn.entity.IGSNEventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@EnableAutoConfiguration
@ContextConfiguration(classes = { IGSNRequestService.class, RequestService.class, ApplicationProperties.class,
		RequestMapper.class, ModelMapper.class })
@TestPropertySource("classpath:application.properties")
class IGSNRequestServiceTest {

	@MockBean
	private RequestRepository repository;

	@Autowired
	private IGSNRequestService service;

	@Autowired
	RequestMapper requestMapper;

	@Test
	void findById_foundRecord_returnsIGSNServiceRequest() {
		Request request = new Request();
		when(repository.findById(any(UUID.class))).thenReturn(Optional.of(request));

		Request actual = service.findById(UUID.randomUUID().toString());

		verify(repository, times(1)).findById(any(UUID.class));
		assertThat(actual).isNotNull();
	}

	@Test
	void findById_notfound_returnsNull() {
		Request actual = service.findById(UUID.randomUUID().toString());

		verify(repository, times(1)).findById(any(UUID.class));
		assertThat(actual).isNull();
	}

}