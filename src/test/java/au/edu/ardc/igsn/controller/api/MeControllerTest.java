package au.edu.ardc.igsn.controller.api;

import au.edu.ardc.igsn.model.User;
import au.edu.ardc.igsn.service.KeycloakService;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import java.util.Arrays;
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

    private final String baseUrl = "/api/me/";

    @Autowired
    MockMvc mockMvc;

    @MockBean
    KeycloakService kcServiceMock;

    @Test
    public void it_should_return_me() throws Exception {

        // mock a user access
        UUID userUUID = UUID.randomUUID();
        User user = new User(userUUID);
        user.setRoles(Arrays.asList("IGSN_USER", "IGSN_ADMIN"));
        user.setName("Minh Duc Nguyen");
        user.setEmail("minh.nguyen@ardc.edu.au");

        // mock a user resources
        List<Permission> permissions = new ArrayList<>();
        Permission permission = new Permission();
        UUID res1 = UUID.randomUUID();
        permission.setResourceId(res1.toString());
        permission.setResourceName("Resource 1");
        permissions.add(permission);
        user.setAllocations(permissions);

        when(kcServiceMock.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(user);

        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.get(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        // it should be ok and the data be updated
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userUUID.toString()));
    }
}