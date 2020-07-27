package au.edu.ardc.igsn.controller.api;

import au.edu.ardc.igsn.KeycloakIntegrationTest;
import org.junit.jupiter.api.Test;


class MeControllerIT extends KeycloakIntegrationTest {

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

}