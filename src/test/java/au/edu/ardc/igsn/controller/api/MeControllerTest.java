package au.edu.ardc.igsn.controller.api;

import au.edu.ardc.igsn.service.KeycloakService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.authorization.Permission;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class MeControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    KeycloakService kcServiceMock;

    @Test
    public void it_should_return_me() throws Exception {

        // mock a user access
        UUID userUUID = UUID.randomUUID();
        AccessToken token = new AccessToken();
        token.setSubject(userUUID.toString());
        token.setPreferredUsername("minhd");
        token.setName("Minh Duc Nguyen");
        token.setEmail("minh.nguyen@ardc.edu.au");
        token.setRealmAccess(new AccessToken.Access().addRole("IGSN_USER"));
        ArrayList<String> groups = new ArrayList<>();
        groups.add("Developer");
        token.setOtherClaims("groups", groups);
        when(kcServiceMock.getAccessToken(any(HttpServletRequest.class))).thenReturn(token);

        // mock a user resources
        List<Permission> permissions = new ArrayList<>();
        Permission permission = new Permission();
        UUID res1 = UUID.randomUUID();
        permission.setResourceId(res1.toString());
        permission.setResourceName("Resource 1");
        permissions.add(permission);
        when(kcServiceMock.getPlainAccessToken(any(HttpServletRequest.class))).thenReturn("asdf");
        when(kcServiceMock.getAuthorizedResources(any(String.class))).thenReturn(permissions);

        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.get("/api/me/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        // it should be ok and the data be updated
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userUUID.toString()))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.groups").isArray())
                .andExpect(jsonPath("$.allocations").isArray())
                .andExpect(jsonPath("$.roles").isNotEmpty())
                .andExpect(jsonPath("$.groups").isNotEmpty())
                .andExpect(jsonPath("$.allocations").isNotEmpty())
        ;
    }
}