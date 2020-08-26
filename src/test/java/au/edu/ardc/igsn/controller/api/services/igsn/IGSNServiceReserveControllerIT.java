package au.edu.ardc.igsn.controller.api.services.igsn;

import au.edu.ardc.igsn.KeycloakIntegrationTest;
import au.edu.ardc.igsn.entity.Identifier;
import au.edu.ardc.igsn.repository.IdentifierRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class IGSNServiceReserveControllerIT extends KeycloakIntegrationTest {

    private final String baseUrl = "/api/services/igsn/reserve";

    @Autowired
    IdentifierRepository identifierRepository;

    @Test
    void reserve_NotLoggedIn_401() {
        this.webTestClient
                .post().uri(baseUrl)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void reserve_validRequest_producesReservedIGSN200() {
        String requestBody = "12073/XXAA1234567\n12703/XXAB12345";
        this.webTestClient
                .post().uri(
                uriBuilder -> uriBuilder
                        .path(baseUrl)
                        .queryParam("allocationID", resourceID)
                        .build())
                .header("Authorization", getBasicAuthenticationHeader(username, password))
                .body(Mono.just(requestBody), String.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.status").exists();

        // 2 identifiers are created
        assertThat(identifierRepository.existsByTypeAndValue(Identifier.Type.IGSN, "12073/XXAA1234567"));
        assertThat(identifierRepository.existsByTypeAndValue(Identifier.Type.IGSN, "12703/XXAB12345"));

        // they are in reserved status
        Identifier identifier = identifierRepository.findByValueAndType("12073/XXAA1234567", Identifier.Type.IGSN);
        assertThat(identifier.getStatus()).isEqualTo(Identifier.Status.RESERVED);
    }
}