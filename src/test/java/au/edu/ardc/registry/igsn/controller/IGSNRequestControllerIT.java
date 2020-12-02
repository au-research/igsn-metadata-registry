package au.edu.ardc.registry.igsn.controller;

import au.edu.ardc.registry.KeycloakIntegrationTest;
import au.edu.ardc.registry.TestHelper;
import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.service.KeycloakService;
import au.edu.ardc.registry.common.service.RequestService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@EnabledIf(expression = "${app.igsn.enabled}", reason = "Disable test if IGSN is not enabled", loadContext = true)
class IGSNRequestControllerIT extends KeycloakIntegrationTest {

    @MockBean
    KeycloakService kcService;

    @MockBean
    RequestService requestService;

    @Test
    public void check_is_request_is_completed_or_failed(){
        Request request = TestHelper.mockRequest();
        when(requestService.findOwnedById(any(), any())).thenReturn(request);
        this.webTestClient.put().uri("api/resources/igsn-requests/434343434?status=RESTART")
                .header("Authorization", getBasicAuthenticationHeader(username, password))
                .header("Content-Type", "application/xml").exchange().expectStatus().isForbidden();
    }

}