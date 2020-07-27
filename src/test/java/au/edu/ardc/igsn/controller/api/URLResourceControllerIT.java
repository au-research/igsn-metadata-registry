package au.edu.ardc.igsn.controller.api;

import au.edu.ardc.igsn.KeycloakIntegrationTest;
import au.edu.ardc.igsn.TestHelper;
import au.edu.ardc.igsn.dto.RecordDTO;
import au.edu.ardc.igsn.dto.URLDTO;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.repository.RecordRepository;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static org.exparity.hamcrest.date.DateMatchers.sameDay;
import static org.junit.jupiter.api.Assertions.*;

class URLResourceControllerIT extends KeycloakIntegrationTest {

    @Autowired
    RecordRepository recordRepository;

    private final String resourceBaseUrl = "/api/resources/urls/";

    @Test
    void store_NotLoggedIn_401() {
        this.webTestClient
                .post().uri(resourceBaseUrl)
                .exchange()
                .expectStatus().isUnauthorized();
    }


    // todo store_RecordNotOwned_403
    // todo store_ValidRecord_202
    // todo store_ImportScope_202Overwrite

    // todo store_RecordNotFound_404
    @Test
    void store_RecordNotFound_404() {
        // given a request dto
        URLDTO dto = new URLDTO();
        dto.setRecord(UUID.randomUUID());
        dto.setUrl("https://researchdata.edu.au/");

        // when POST, record is not found
        this.webTestClient
                .post().uri(resourceBaseUrl)
                .header("Authorization", getBasicAuthenticationHeader(username, password))
                .body(Mono.just(dto), URLDTO.class)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void store_Forbidden_403() {
        // given a record of a different owner
        Record record = TestHelper.mockRecord();
        recordRepository.saveAndFlush(record);

        // given a request dto
        URLDTO dto = new URLDTO();
        dto.setRecord(record.getId());
        dto.setUrl("https://researchdata.edu.au/");

        // when POST, oepration is not forbidden
        this.webTestClient
                .post().uri(resourceBaseUrl)
                .header("Authorization", getBasicAuthenticationHeader(username, password))
                .body(Mono.just(dto), URLDTO.class)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void store_Valid_201() {
        // given a record of a different owner
        Record record = TestHelper.mockRecord();
        record.setOwnerType(Record.OwnerType.User);
        record.setOwnerID(UUID.fromString(userID));
        recordRepository.saveAndFlush(record);

        // given a request dto
        URLDTO dto = new URLDTO();
        dto.setRecord(record.getId());
        dto.setUrl("https://researchdata.edu.au/");

        // when POST, oepration is not forbidden
        this.webTestClient
                .post().uri(resourceBaseUrl)
                .header("Authorization", getBasicAuthenticationHeader(username, password))
                .body(Mono.just(dto), URLDTO.class)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("Location")
                .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.createdAt").exists();
    }

    @Test
    void store_ImportScope_201Overwrite() throws ParseException {
        // given a record of a different owner
        Record record = TestHelper.mockRecord();
        record.setOwnerType(Record.OwnerType.User);
        record.setOwnerID(UUID.fromString(userID));
        recordRepository.saveAndFlush(record);

        // given a request dto
        URLDTO dto = new URLDTO();
        dto.setRecord(record.getId());
        Date expectedDate = new SimpleDateFormat("dd/MM/yyyy").parse("02/02/1989");
        dto.setCreatedAt(expectedDate);
        dto.setUrl("https://researchdata.edu.au/");

        // when POST, expects 201, Location Header, and the ID in the body
        this.webTestClient
                .post().uri(resourceBaseUrl)
                .header("Authorization", getBasicAuthenticationHeader(username, password))
                .body(Mono.just(dto), URLDTO.class)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("Location")
                .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.createdAt").exists()
                .jsonPath("$.createdAt", sameDay(expectedDate)).hasJsonPath();
    }
}