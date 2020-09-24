package au.edu.ardc.registry.common.controller.api.resources;

import au.edu.ardc.registry.TestHelper;
import au.edu.ardc.registry.common.config.ApplicationProperties;
import au.edu.ardc.registry.common.config.WebConfig;
import au.edu.ardc.registry.common.controller.APIRestControllerAdvice;
import au.edu.ardc.registry.common.dto.RecordDTO;
import au.edu.ardc.registry.common.dto.mapper.RecordMapper;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.service.*;
import au.edu.ardc.registry.exception.ForbiddenOperationException;
import au.edu.ardc.registry.exception.RecordNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.annotation.ControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

import static au.edu.ardc.registry.TestHelper.asJsonString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(RecordResourceController.class)
@Import(RecordResourceController.class)
@ContextConfiguration(classes = { RecordMapper.class, ModelMapper.class, APIRestControllerAdvice.class })
@AutoConfigureMockMvc
public class RecordResourceControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	RecordMapper mapper;

	@MockBean
	KeycloakService kcService;

	@MockBean
	RecordService recordService;

	@MockBean
	IdentifierService identifierService;

	@MockBean
	VersionService versionService;

	@MockBean
	ApplicationProperties applicationProperties;

	@Test
	public void show_recordDoesNotExist_404() throws Exception {
		User user = TestHelper.mockUser();
		Record record = TestHelper.mockRecord(UUID.randomUUID());

		when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(user);
		when(recordService.findOwnedById(record.getId().toString(), user)).thenThrow(RecordNotFoundException.class);

		MockHttpServletRequestBuilder request = MockMvcRequestBuilders
				.get("/api/resources/records/" + record.getId().toString()).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);

		mockMvc.perform(request).andExpect(status().isNotFound());
	}

	@Test
	public void show_recordExists_returnsDTO() throws Exception {
		User user = TestHelper.mockUser();
		Record record = TestHelper.mockRecord(UUID.randomUUID());

		when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(user);
		when(recordService.findOwnedById(record.getId().toString(), user)).thenReturn(record);

		MockHttpServletRequestBuilder request = MockMvcRequestBuilders
				.get("/api/resources/records/" + record.getId().toString()).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);

		mockMvc.perform(request).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(record.getId().toString()));
	}

	@Test
	public void store_UserSufficientPermission_200() throws Exception {

		User user = TestHelper.mockUser();
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		record.setOwnerID(user.getId());
		record.setAllocationID(UUID.randomUUID());

		// given a creator with an allocation and a proposed datacenter
		when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(user);
		when(recordService.create(any(RecordDTO.class), any(User.class))).thenReturn(record);

		// when POST to the records endpoint with the allocationID and datacenterID
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post("/api/resources/records/")
				.content(asJsonString(mapper.convertToDTO(record))).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);

		mockMvc.perform(request).andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(record.getId().toString()))
				.andExpect(jsonPath("$.createdAt").exists()).andExpect(jsonPath("$.allocationID").exists())
				.andExpect(jsonPath("$.allocationID").exists())
				.andExpect(jsonPath("$.ownerID").value(user.getId().toString()))
				.andExpect(jsonPath("$.ownerType").value(Record.OwnerType.User.toString()));
	}

	@Test
	public void store_UserInsufficientPermission_403() throws Exception {
		User user = new User(UUID.randomUUID());
		when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(user);
		when(recordService.create(any(RecordDTO.class), any(User.class))).thenThrow(ForbiddenOperationException.class);

		Record actual = TestHelper.mockRecord();

		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post("/api/resources/records/")
				.content(asJsonString(actual)).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);

		mockMvc.perform(request).andExpect(status().isForbidden());
	}

	@Test
	public void update_UserSufficientPermission_202() throws Exception {
		// given a record
		Record record = TestHelper.mockRecord(UUID.randomUUID());

		// setting up the world
		when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(TestHelper.mockUser());
		when(recordService.update(any(RecordDTO.class), any(User.class))).thenReturn(record);

		// when PUT to the record endpoint
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders
				.put("/api/resources/records/" + record.getId().toString())
				.content(asJsonString(mapper.convertToDTO(record))).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);

		// should returns an accepted header with the dto returned
		mockMvc.perform(request).andExpect(status().isAccepted())
				.andExpect(jsonPath("$.id").value(record.getId().toString()));

		// service.update is called
		Mockito.verify(recordService, times(1)).update(any(RecordDTO.class), any(User.class));
	}

	@Test
	public void delete_UserSufficientPermission_202() throws Exception {
		// given a record
		Record record = TestHelper.mockRecord(UUID.randomUUID());

		// given a user that has access to said allocation update scope
		User user = TestHelper.mockUser();

		when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(user);
		when(recordService.exists(record.getId().toString())).thenReturn(true);
		when(recordService.delete(record.getId().toString(), user)).thenReturn(true);

		// when delete
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders
				.delete("/api/resources/records/" + record.getId().toString());

		// it should be accepted
		mockMvc.perform(request).andExpect(status().isAccepted());
	}

}