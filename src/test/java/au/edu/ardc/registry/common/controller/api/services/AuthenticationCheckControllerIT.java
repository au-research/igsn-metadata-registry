package au.edu.ardc.registry.common.controller.api.services;

import au.edu.ardc.registry.KeycloakIntegrationTest;
import au.edu.ardc.registry.TestHelper;
import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.repository.IdentifierRepository;
import au.edu.ardc.registry.common.repository.RecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

class AuthenticationCheckControllerIT extends KeycloakIntegrationTest {

    final String baseUrl = "/api/services/auth-check/";

    @Autowired
    RecordRepository recordRepository;

    @Autowired
    IdentifierRepository identifierRepository;

    @Test
    void validateOwnership_notLoggedIn_401() {
        this.webTestClient.get().uri(baseUrl).exchange().expectStatus().isUnauthorized();
    }

    @Test
    void validateOwnership_loggedInNoIdentifier_404() {
        this.webTestClient.get().
                uri(uriBuilder -> uriBuilder
                        .path(baseUrl)
                        .queryParam("identifier", "10273/XXAA" + UUID.randomUUID().toString())
                        .build())
                .header("Authorization", getBasicAuthenticationHeader(username, password))
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void validateOwnership_loggedInNotOwned_403() {
        // given a record, and an identifier
        Record record = TestHelper.mockRecord();
        recordRepository.saveAndFlush(record);
        Identifier identifier = TestHelper.mockIdentifier(record);
        identifier.setType(Identifier.Type.IGSN);
        identifier.setValue("10273/XXAA"+ UUID.randomUUID().toString());
        identifierRepository.saveAndFlush(identifier);

        this.webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path(baseUrl)
                .queryParam("identifier", identifier.getValue())
                .build())
                .header("Authorization", getBasicAuthenticationHeader(username, password))
                .exchange().expectStatus().isForbidden();
    }

    @Test
    void validateOwnership_loggedInOwned_200() {
        // given an owned record, and an identifier
        Record record = TestHelper.mockRecord();
        record.setOwnerType(Record.OwnerType.User);
        record.setOwnerID(UUID.fromString(userID));
        recordRepository.saveAndFlush(record);
        Identifier identifier = TestHelper.mockIdentifier(record);
        identifier.setType(Identifier.Type.IGSN);
        identifier.setValue("10273/XXAA"+ UUID.randomUUID().toString());
        identifierRepository.saveAndFlush(identifier);

        this.webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(baseUrl)
                        .queryParam("identifier", identifier.getValue())
                        .build())
                .header("Authorization", getBasicAuthenticationHeader(username, password))
                .exchange().expectStatus().isOk();
    }
}