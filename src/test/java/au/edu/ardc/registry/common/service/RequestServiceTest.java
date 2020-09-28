package au.edu.ardc.registry.common.service;

import au.edu.ardc.registry.TestHelper;
import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.repository.RequestRepository;
import au.edu.ardc.registry.common.repository.specs.RequestSpecification;
import au.edu.ardc.registry.exception.ForbiddenOperationException;
import au.edu.ardc.registry.exception.RequestNotFoundException;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { RequestService.class })
class RequestServiceTest {

	@Autowired
	RequestService requestService;

	@MockBean
	RequestRepository requestRepository;

	@Test
	@DisplayName("When findOwnedById, throws RequestNotFound when the uuid is not found")
	void findOwnedById_throwsRequestNotFound() {
		when(requestRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
		Assert.assertThrows(RequestNotFoundException.class, () -> {
			requestService.findOwnedById(UUID.randomUUID().toString(), TestHelper.mockUser());
		});
		when(requestRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
	}

	@Test
	@DisplayName("When findOwnedById, throws ForbiddenOperation when the user id doesn't match the creator id")
	void findOwnedById_throwsForbiddenOperation() {
		User user = TestHelper.mockUser();
		when(requestRepository.findById(any(UUID.class))).thenReturn(Optional.of(TestHelper.mockRequest()));
		Assert.assertThrows(ForbiddenOperationException.class, () -> {
			requestService.findOwnedById(UUID.randomUUID().toString(), user);
		});
	}

	@Test
	void findById() {
		when(requestRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
		assertThat(requestService.findById(UUID.randomUUID().toString())).isNull();

		when(requestRepository.findById(any(UUID.class))).thenReturn(Optional.of(TestHelper.mockRequest()));
		assertThat(requestService.findById(UUID.randomUUID().toString())).isNotNull();
		assertThat(requestService.findById(UUID.randomUUID().toString())).isInstanceOf(Request.class);
	}

	@Test
	@DisplayName("Search calls findAll in repository")
	void search() {
		requestService.search(new RequestSpecification(), PageRequest.of(0, 10));
		verify(requestRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
	}
}