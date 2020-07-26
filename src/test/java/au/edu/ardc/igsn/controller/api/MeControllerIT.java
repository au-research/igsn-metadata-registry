package au.edu.ardc.igsn.controller.api;

import au.edu.ardc.igsn.IGSNMetadataRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.xml.bind.DatatypeConverter;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {IGSNMetadataRegistry.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIf(expression = "${keycloak.enabled}", reason = "Disable test if keycloak is not enabled", loadContext = true)
@ActiveProfiles("integration")
@AutoConfigureWebTestClient
public class MeControllerIT {

    @Value("${test.kc.user.username}")
    private String username;

    @Value("${test.kc.user.password}")
    private String password;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void whoami_NotLoggedIn_401() {
        this.webTestClient
                .get().uri("/api/me/")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    public void whoami_LoggedIn_showUserProfile() {
        this.webTestClient
                .get().uri("/api/me/")
                .header("Authorization", getBasicAuthenticationHeader(username, password))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.email").exists();
    }

    private String getBasicAuthenticationHeader(String username, String password) {
        return "Basic " + DatatypeConverter.printBase64Binary((username + ":" + password).getBytes());
    }
}