package au.edu.ardc.registry.common.controller.api.resources;

import au.edu.ardc.registry.TestHelper;
import au.edu.ardc.registry.common.config.WebConfig;
import au.edu.ardc.registry.common.dto.IdentifierDTO;
import au.edu.ardc.registry.common.dto.URLDTO;
import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.service.APILoggingService;
import au.edu.ardc.registry.common.service.IdentifierService;
import au.edu.ardc.registry.common.service.KeycloakService;
import au.edu.ardc.registry.common.service.RecordService;
import au.edu.ardc.registry.exception.ForbiddenOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = {IdentifierResourceController.class})
@Import(APILoggingService.class)
@AutoConfigureMockMvc
public class IdentifierResourceControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockBean
	IdentifierService service;

	@MockBean
	RecordService recordService;

	@MockBean
	KeycloakService kcService;

	@Test
	public void store_RecordNotOwned_403() throws Exception {
		// given a user & record, no relation
		User user = TestHelper.mockUser();
		Record record = TestHelper.mockRecord(UUID.randomUUID());

		// and a request dto
		IdentifierDTO dto = new IdentifierDTO();
		dto.setRecord(record.getId());

		// when service throws Forbidden, it bubbles up
		when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(user);
		when(service.create(any(IdentifierDTO.class), any(User.class))).thenThrow(ForbiddenOperationException.class);

		// when attempt to create an identifier
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post("/api/resources/identifiers/")
				.content(TestHelper.asJsonString(dto)).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);

		mockMvc.perform(request).andExpect(status().isForbidden());
	}

	@Test
	public void store_validRequest_returnsDTO() throws Exception {
		IdentifierDTO resultDTO = new IdentifierDTO();
		resultDTO.setId(UUID.randomUUID());

		// mock a valid return from the service
		when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(TestHelper.mockUser());
		when(service.create(any(IdentifierDTO.class), any(User.class))).thenReturn(resultDTO);

		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post("/api/resources/identifiers/")
				.content(TestHelper.asJsonString(new URLDTO())).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);

		mockMvc.perform(request).andExpect(status().isCreated()).andExpect(jsonPath("$.id").exists());
	}

	@Test
	public void it_should_return_an_identifier_when_get_by_id() throws Exception {
		Identifier identifier = TestHelper.mockIdentifier(UUID.randomUUID());
		when(service.findById(identifier.getId().toString())).thenReturn(identifier);

		MockHttpServletRequestBuilder request = MockMvcRequestBuilders
				.get("/api/resources/identifiers/" + identifier.getId().toString())
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);

		mockMvc.perform(request).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(identifier.getId().toString()));
	}

	@Test
	public void it_should_404_when_get_by_non_existence_uuid() throws Exception {
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders
				.get("/api/resources/identifiers/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);

		mockMvc.perform(request).andExpect(status().isNotFound());
	}

	@Test
	public void it_should_404_when_delete_by_non_existence_uuid() throws Exception {
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders
				.delete("/api/resources/identifiers/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);

		mockMvc.perform(request).andExpect(status().isNotFound());
	}

}
