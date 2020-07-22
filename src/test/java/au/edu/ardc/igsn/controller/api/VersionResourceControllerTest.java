package au.edu.ardc.igsn.controller.api;

import au.edu.ardc.igsn.Scope;
import au.edu.ardc.igsn.TestHelper;
import au.edu.ardc.igsn.User;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.entity.Version;
import au.edu.ardc.igsn.service.KeycloakService;
import au.edu.ardc.igsn.service.RecordService;
import au.edu.ardc.igsn.service.VersionService;
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
import java.util.Date;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class VersionResourceControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    VersionService service;

    @MockBean
    RecordService recordService;

    @MockBean
    KeycloakService kcService;

    @Test
    public void it_should_return_a_version_when_get_by_id() throws Exception {
        Version version = TestHelper.mockVersion(UUID.randomUUID());
        when(service.findById(version.getId().toString())).thenReturn(version);

        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.get("/api/resources/versions/" + version.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(version.getId().toString()));
    }

    @Test
    public void it_should_404_when_get_by_non_existence_uuid() throws Exception {
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.get("/api/resources/versions/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isNotFound());
    }

    @Test
    public void it_should_404_when_delete_by_non_existence_uuid() throws Exception {
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.delete("/api/resources/versions/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isNotFound());
    }

    @Test
    public void it_should_delete_a_version_when_delete_by_by_uuid() throws Exception {
        Version version = TestHelper.mockVersion();

        Version endedVersion = TestHelper.mockVersion(version.getId());
        endedVersion.setStatus(Version.Status.SUPERSEDED);
        endedVersion.setEndedAt(new Date());

        when(service.exists(version.getId().toString())).thenReturn(true);
        when(service.findById(version.getId().toString())).thenReturn(version);
        when(service.end(version)).thenReturn(endedVersion);

        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.delete("/api/resources/versions/" + version.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value(Version.Status.SUPERSEDED.toString()))
        ;
    }

    @Test
    public void it_should_400_when_creating_a_version_with_an_unsupported_schema() throws Exception {
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.post("/api/resources/versions/")
                        .param("recordID", UUID.randomUUID().toString())
                        .param("schemaID", "not-supported-schema")
                        .content(TestHelper.asJsonString(TestHelper.mockVersion()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request).andExpect(status().isBadRequest());
    }

    @Test
    public void it_should_404_when_creating_a_version_with_an_unknown_record() throws Exception {
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.post("/api/resources/versions/")
                        .param("recordID", UUID.randomUUID().toString())
                        .param("schemaID", "igsn-registration-v1")
                        .content(TestHelper.asJsonString(TestHelper.mockVersion()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request).andExpect(status().isNotFound());
    }

    @Test
    public void it_should_400_when_the_logged_in_user_does_not_have_access_to_the_resource() throws Exception {
        // given a user with no allocation
        User john = TestHelper.mockUser();

        // and a record that is owned by John
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        record.setOwnerID(john.getId());

        Version version = TestHelper.mockVersion();
        version.setRecord(record);

        when(recordService.exists(record.getId().toString())).thenReturn(true);
        when(recordService.findById(record.getId().toString())).thenReturn(record);
        when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(john);

        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.post("/api/resources/versions/")
                        .param("recordID", record.getId().toString())
                        .param("schemaID", "igsn-registration-v1")
                        .content("random text")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request).andExpect(status().isForbidden());
    }

    @Test
    public void it_should_400_when_the_user_has_the_wrong_scope_to_the_resource() throws Exception {
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
                MockMvcRequestBuilders.post("/api/resources/versions/")
                        .param("recordID", record.getId().toString())
                        .param("schemaID", "igsn-registration-v1")
                        .content("some content")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request).andExpect(status().isForbidden());
    }

    @Test
    public void it_should_create_a_version_when_post() throws Exception {
        UUID allocationID = UUID.randomUUID();

        // given a logged in user with the right scope and permission
        User john = TestHelper.mockUser();
        TestHelper.addResourceAndScopePermissionToUser(john, allocationID.toString(), Sets.newHashSet(Scope.CREATE.getValue()));

        // and a record that is owned by John
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        record.setOwnerID(john.getId());
        record.setAllocationID(allocationID);

        Version version = TestHelper.mockVersion();

        when(service.create(any(Version.class))).thenReturn(version);
        when(recordService.exists(record.getId().toString())).thenReturn(true);
        when(recordService.findById(record.getId().toString())).thenReturn(record);
        when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(john);

        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.post("/api/resources/versions/")
                        .param("recordID", record.getId().toString())
                        .param("schemaID", "igsn-registration-v1")
                        .content(TestHelper.asJsonString(version))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

}