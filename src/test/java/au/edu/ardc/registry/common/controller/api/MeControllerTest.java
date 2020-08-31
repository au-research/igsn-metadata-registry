package au.edu.ardc.registry.common.controller.api;

import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.service.KeycloakService;
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
import java.util.Arrays;
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