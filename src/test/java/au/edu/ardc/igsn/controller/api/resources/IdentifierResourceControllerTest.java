package au.edu.ardc.igsn.controller.api.resources;

import au.edu.ardc.igsn.dto.IdentifierDTO;
import au.edu.ardc.igsn.dto.URLDTO;
import au.edu.ardc.igsn.exception.ForbiddenOperationException;
import au.edu.ardc.igsn.model.Scope;
import au.edu.ardc.igsn.TestHelper;
import au.edu.ardc.igsn.model.User;
import au.edu.ardc.igsn.entity.Identifier;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.service.KeycloakService;
import au.edu.ardc.igsn.service.RecordService;
import au.edu.ardc.igsn.service.IdentifierService;
import com.google.common.collect.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
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
        when(kcService.getLoggedInUser(any(HttpServletRequest.class)))
                .thenReturn(user);
        when(service.create(any(IdentifierDTO.class), any(User.class)))
                .thenThrow(ForbiddenOperationException.class);

        // when attempt to create an identifier
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.post("/api/resources/identifiers/")
                        .content(TestHelper.asJsonString(dto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request).andExpect(status().isForbidden());
    }

    @Test
    public void store_validRequest_returnsDTO() throws Exception {
        IdentifierDTO resultDTO = new IdentifierDTO();
        resultDTO.setId(UUID.randomUUID());

        // mock a valid return from the service
        when(kcService.getLoggedInUser(any(HttpServletRequest.class)))
                .thenReturn(TestHelper.mockUser());
        when(service.create(any(IdentifierDTO.class), any(User.class)))
                .thenReturn(resultDTO);

        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.post("/api/resources/identifiers/")
                        .content(TestHelper.asJsonString(new URLDTO()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    public void it_should_return_an_identifier_when_get_by_id() throws Exception {
        Identifier identifier = TestHelper.mockIdentifier(UUID.randomUUID());
        when(service.findById(identifier.getId().toString())).thenReturn(identifier);

        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.get("/api/resources/identifiers/" + identifier.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(identifier.getId().toString()));
    }

    @Test
    public void it_should_404_when_get_by_non_existence_uuid() throws Exception {
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.get("/api/resources/identifiers/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isNotFound());
    }

    @Test
    public void it_should_404_when_delete_by_non_existence_uuid() throws Exception {
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.delete("/api/resources/identifiers/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isNotFound());
    }

    @Test
    public void it_should_update_an_identifier_when_put() throws Exception {
        UUID allocationID = UUID.randomUUID();

        // given a logged in user with the right scope and permission
        User john = TestHelper.mockUser();
        TestHelper.addResourceAndScopePermissionToUser(john, allocationID.toString(), Sets.newHashSet(Scope.UPDATE.getValue()));

        // and a record that is owned by John
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        record.setOwnerID(john.getId());
        record.setAllocationID(allocationID);

        Identifier identifier = TestHelper.mockIdentifier(UUID.randomUUID());
        identifier.setRecord(record);

        when(recordService.exists(record.getId().toString())).thenReturn(true);
        when(service.exists(identifier.getId().toString())).thenReturn(true);
        when(service.update(any(Identifier.class))).thenReturn(identifier);
        when(service.findById(identifier.getId().toString())).thenReturn(identifier);
        when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(john);

        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.put("/api/resources/identifiers/" + identifier.getId().toString())
                        .content(TestHelper.asJsonString(identifier))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        // it should be ok and the data be updated
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
    }
}