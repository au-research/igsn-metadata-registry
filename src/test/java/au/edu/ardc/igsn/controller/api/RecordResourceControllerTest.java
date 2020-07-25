package au.edu.ardc.igsn.controller.api;

import au.edu.ardc.igsn.Scope;
import au.edu.ardc.igsn.TestHelper;
import au.edu.ardc.igsn.User;
import au.edu.ardc.igsn.dto.RecordDTO;
import au.edu.ardc.igsn.dto.RecordMapper;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.exception.ForbiddenOperationException;
import au.edu.ardc.igsn.exception.RecordNotFoundException;
import au.edu.ardc.igsn.service.KeycloakService;
import au.edu.ardc.igsn.service.RecordService;
import com.google.common.collect.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
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
import java.util.ArrayList;
import java.util.UUID;

import static au.edu.ardc.igsn.TestHelper.asJsonString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class RecordResourceControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    RecordMapper mapper;

    @MockBean
    RecordService service;

    @MockBean
    KeycloakService kcService;

    @Test
    public void it_should_return_all_records_when_get() throws Exception {
        // todo refactor this test
        // given a creator with 2 records
        UUID creatorID = UUID.randomUUID();

        ArrayList<Record> expected = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Record record = TestHelper.mockRecord();
            record.setCreatorID(creatorID);
            expected.add(record);
        }
        when(service.findOwned(any())).thenReturn(expected);

        // probably not need this here, because we already mock out the service response
        when(kcService.getUserUUID(any(HttpServletRequest.class))).thenReturn(creatorID);

        // when GET /
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.get("/api/resources/records/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        // returns 2 records
        mockMvc.perform(request)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").exists());
    }

    @Test
    public void show_recordDoesNotExist_404() throws Exception {
        User user = TestHelper.mockUser();
        Record record = TestHelper.mockRecord(UUID.randomUUID());

        when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(user);
        when(service.findById(record.getId().toString(), user)).thenThrow(RecordNotFoundException.class);

        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.get("/api/resources/records/" + record.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request).andExpect(status().isNotFound());
    }

    @Test
    public void show_recordExists_returnsDTO() throws Exception {
        User user = TestHelper.mockUser();
        Record record = TestHelper.mockRecord(UUID.randomUUID());

        when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(user);
        when(service.findById(record.getId().toString(), user)).thenReturn(mapper.convertToDTO(record));

        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.get("/api/resources/records/" + record.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(record.getId().toString()));
    }

    @Test
    public void store_UserSufficientPermission_200() throws Exception {

        User user = TestHelper.mockUser();
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        record.setOwnerID(user.getId());
        record.setAllocationID(UUID.randomUUID());
        RecordDTO dto = mapper.convertToDTO(record);

        // given a creator with an allocation and a proposed datacenter
        when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(user);
        when(service.create(any(RecordDTO.class), any(User.class))).thenReturn(dto);

        // when POST to the records endpoint with the allocationID and datacenterID
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.post("/api/resources/records/")
                        .content(asJsonString(dto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(record.getId().toString()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.allocationID").exists())
                .andExpect(jsonPath("$.allocationID").exists())
                .andExpect(jsonPath("$.ownerID").value(user.getId().toString()))
                .andExpect(jsonPath("$.ownerType").value(Record.OwnerType.User.toString()))
        ;
    }

    @Test
    public void store_UserInsufficientPermission_403() throws Exception {
        User user = new User(UUID.randomUUID());
        when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(user);
        when(service.create(any(RecordDTO.class), any(User.class))).thenThrow(ForbiddenOperationException.class);

        Record actual = TestHelper.mockRecord();

        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.post("/api/resources/records/")
                        .content(asJsonString(actual))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isForbidden());
    }

    @Test
    public void update_UserSufficientPermission_202() throws Exception {
        // given a record
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        RecordDTO dto = mapper.convertToDTO(record);
        dto.setStatus(Record.Status.DRAFT);

        // setting up the world
        when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(TestHelper.mockUser());
        when(service.update(any(RecordDTO.class), any(User.class))).thenReturn(dto);

        // when PUT to the record endpoint
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.put("/api/resources/records/" + record.getId().toString())
                        .content(asJsonString(dto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        // should returns an accepted header with the dto returned
        mockMvc.perform(request)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(record.getId().toString()));

        // service.update is called
        Mockito.verify(service, times(1)).update(any(RecordDTO.class), any(User.class));
    }

    @Test
    public void delete_UserSufficientPermission_202() throws Exception {
        // given a record
        Record record = TestHelper.mockRecord(UUID.randomUUID());

        // given a user that has access to said allocation update scope
        User user = TestHelper.mockUser();
        TestHelper.addResourceAndScopePermissionToUser(user, record.getAllocationID().toString(), Sets.newHashSet(Scope.UPDATE.getValue()));

        when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(user);
        when(service.exists(record.getId().toString())).thenReturn(true);
        when(service.delete(record.getId().toString(), user)).thenReturn(true);

        // when delete
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.delete("/api/resources/records/" + record.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        // it should be accepted
        mockMvc.perform(request)
                .andExpect(status().isAccepted());
    }
}