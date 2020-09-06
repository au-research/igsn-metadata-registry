package au.edu.ardc.registry.common.controller.api.resources;

import au.edu.ardc.registry.TestHelper;
import au.edu.ardc.registry.common.config.RequestLoggingFilter;
import au.edu.ardc.registry.common.config.WebConfig;
import au.edu.ardc.registry.common.dto.VersionDTO;
import au.edu.ardc.registry.common.dto.mapper.VersionMapper;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.service.KeycloakService;
import au.edu.ardc.registry.common.service.VersionService;
import au.edu.ardc.registry.exception.ForbiddenOperationException;
import au.edu.ardc.registry.exception.RecordNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.servlet.http.HttpServletRequest;
import java.util.Base64;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = VersionResourceController.class,
		excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
				classes = { RequestLoggingFilter.class, WebConfig.class }))
public class VersionResourceControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockBean
	VersionService service;

	@MockBean
	KeycloakService kcService;

	@MockBean
	VersionMapper versionMapper;

	// todo index
	// todo index_pagination
	// todo show_404
	// todo show
	// todo delete_403
	// todo delete_404
	// todo delete

	@Test
	public void store_UnknownRecord_404() throws Exception {
		// given a dto request
		VersionDTO versionDTO = new VersionDTO();
		versionDTO.setContent(Base64.getEncoder().encode("stuff".getBytes()).toString());
		versionDTO.setRecord(UUID.randomUUID().toString());
		versionDTO.setSchema("igsn-descriptive-v1");

		// setup mocks
		when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(TestHelper.mockUser());
		when(service.create(any(VersionDTO.class), any(User.class))).thenThrow(new RecordNotFoundException("some-id"));

		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post("/api/resources/versions/")
				.content(TestHelper.asJsonString(versionDTO)).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);

		mockMvc.perform(request).andExpect(status().isNotFound());
	}

	@Test
	public void store_forbidden_403() throws Exception {
		// given a dto request
		VersionDTO versionDTO = new VersionDTO();
		versionDTO.setContent(Base64.getEncoder().encode("stuff".getBytes()).toString());
		versionDTO.setRecord(UUID.randomUUID().toString());
		versionDTO.setSchema("igsn-descriptive-v1");

		// setup mocks
		when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(TestHelper.mockUser());
		when(service.create(any(VersionDTO.class), any(User.class)))
				.thenThrow(new ForbiddenOperationException("some-id"));

		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post("/api/resources/versions/")
				.content(TestHelper.asJsonString(versionDTO)).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);

		mockMvc.perform(request).andExpect(status().isForbidden());
	}

	@Test
	public void store_ValidRequest_returns201WithLocation() throws Exception {
		// given a dto request
		VersionDTO versionDTO = new VersionDTO();
		versionDTO.setContent(Base64.getEncoder().encode("stuff".getBytes()).toString());
		versionDTO.setRecord(UUID.randomUUID().toString());
		versionDTO.setSchema("igsn-descriptive-v1");

		// and a dto response
		VersionDTO resultDTO = new VersionDTO();
		resultDTO.setId(UUID.randomUUID().toString());

		// setup mocks
		when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(TestHelper.mockUser());
		when(service.create(any(VersionDTO.class), any(User.class))).thenReturn(versionDTO);

		// when POST
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post("/api/resources/versions/")
				.content(TestHelper.asJsonString(versionDTO)).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);

		// expects 201 and Location header
		mockMvc.perform(request).andExpect(status().isCreated()).andExpect(header().exists("Location"));
	}

	@Test
	public void store_iInvalidRequest_returns400() throws Exception {
		// given an invalid dto request
		VersionDTO versionDTO = new VersionDTO();

		// when POST
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post("/api/resources/versions/")
				.content(TestHelper.asJsonString(versionDTO)).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);

		// expects 400
		mockMvc.perform(request).andExpect(status().isBadRequest());
	}

}