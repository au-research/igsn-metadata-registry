package au.edu.ardc.igsn.controller.api;

import au.edu.ardc.igsn.model.Scope;
import au.edu.ardc.igsn.TestHelper;
import au.edu.ardc.igsn.model.User;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.entity.URL;
import au.edu.ardc.igsn.service.KeycloakService;
import au.edu.ardc.igsn.service.RecordService;
import au.edu.ardc.igsn.service.URLService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class URLResourceControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    URLService service;

    @MockBean
    RecordService recordService;

    @MockBean
    KeycloakService kcService;

    @Test
    public void it_should_return_a_url_when_get_by_id() throws Exception {
        URL url = TestHelper.mockUrl(UUID.randomUUID());
        when(service.findById(url.getId().toString())).thenReturn(url);

        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.get("/api/resources/urls/" + url.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(url.getId().toString()));
    }

    @Test
    public void it_should_404_when_get_by_non_existence_uuid() throws Exception {
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.get("/api/resources/urls/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isNotFound());
    }

    @Test
    public void it_should_404_when_delete_by_non_existence_uuid() throws Exception {
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.delete("/api/resources/urls/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isNotFound());
    }

    @Test
    public void it_should_delete_a_url_when_delete_by_by_uuid() throws Exception {
        URL url = TestHelper.mockUrl();

        when(service.exists(url.getId().toString())).thenReturn(true);
        when(service.findById(url.getId().toString())).thenReturn(url);

        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.delete("/api/resources/urls/" + url.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isAccepted());
    }

    @Test
    public void it_should_404_when_creating_a_url_with_an_unknown_record() throws Exception {
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.post("/api/resources/urls/")
                        .param("recordID", UUID.randomUUID().toString())
                        .content(TestHelper.asJsonString(TestHelper.mockUrl()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request).andExpect(status().isNotFound());
    }

    @Test
    public void it_should_403_when_the_user_has_the_wrong_scope_to_the_resource() throws Exception {
        // given a user with inadequate allocation
        UUID allocationID = UUID.randomUUID();
        User john = TestHelper.mockUser();
        TestHelper.addResourceAndScopePermissionToUser(john, allocationID.toString(), Sets.newHashSet(Scope.UPDATE.toString()));

        // and a record
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        record.setAllocationID(allocationID);

        when(recordService.exists(record.getId().toString())).thenReturn(true);
        when(recordService.findById(record.getId().toString())).thenReturn(record);
        when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(john);

        // when attempt to create a version
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.post("/api/resources/urls/")
                        .param("recordID", record.getId().toString())
                        .content(TestHelper.asJsonString(TestHelper.mockUrl()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request).andExpect(status().isForbidden());
    }

    @Test
    public void it_should_create_a_url_when_post() throws Exception {
        UUID allocationID = UUID.randomUUID();

        // given a logged in user with the right scope and permission
        User john = TestHelper.mockUser();
        TestHelper.addResourceAndScopePermissionToUser(john, allocationID.toString(), Sets.newHashSet(Scope.CREATE.getValue()));

        // and a record that is owned by John
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        record.setOwnerID(john.getId());
        record.setAllocationID(allocationID);

        URL url = TestHelper.mockUrl();

        when(service.create(any(URL.class))).thenReturn(url);
        when(recordService.exists(record.getId().toString())).thenReturn(true);
        when(recordService.findById(record.getId().toString())).thenReturn(record);
        when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(john);

        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.post("/api/resources/urls/")
                        .param("recordID", record.getId().toString())
                        .content(TestHelper.asJsonString(url))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    public void it_should_update_a_url_when_put() throws Exception {
        UUID allocationID = UUID.randomUUID();

        // given a logged in user with the right scope and permission
        User john = TestHelper.mockUser();
        TestHelper.addResourceAndScopePermissionToUser(john, allocationID.toString(), Sets.newHashSet(Scope.UPDATE.getValue()));

        // and a record that is owned by John
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        record.setOwnerID(john.getId());
        record.setAllocationID(allocationID);

        URL url = TestHelper.mockUrl(UUID.randomUUID());
        url.setRecord(record);

        when(recordService.exists(record.getId().toString())).thenReturn(true);
        when(service.exists(url.getId().toString())).thenReturn(true);
        when(service.update(any(URL.class))).thenReturn(url);
        when(service.findById(url.getId().toString())).thenReturn(url);
        when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(john);

        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.put("/api/resources/urls/" + url.getId().toString())
                        .content(TestHelper.asJsonString(url))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        // it should be ok and the data be updated
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
    }

}
