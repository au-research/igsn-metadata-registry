package au.edu.ardc.registry.common.controller.api;

import au.edu.ardc.registry.KeycloakIntegrationTest;
import org.junit.jupiter.api.Test;


class MeControllerIT extends KeycloakIntegrationTest {

    private final String baseUrl = "/api/me/";

    @Test
    void whoami_NotLoggedIn_401() {
        this.webTestClient
                .get().uri(baseUrl)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    public void whoami_LoggedIn_showUserProfile() {
        this.webTestClient
                .get().uri(baseUrl)
                .header("Authorization", getBasicAuthenticationHeader(username, password))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.email").exists();
    }

}