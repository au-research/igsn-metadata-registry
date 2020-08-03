package au.edu.ardc.igsn.controller.api.resources;

import au.edu.ardc.igsn.KeycloakIntegrationTest;
import au.edu.ardc.igsn.TestHelper;
import au.edu.ardc.igsn.dto.VersionDTO;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.repository.RecordRepository;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static org.exparity.hamcrest.date.DateMatchers.sameDay;

class VersionResourceControllerIT extends KeycloakIntegrationTest {

    @Autowired
    RecordRepository repository;

    @Test
    void store_NotLoggedIn_401() {
        // when POST without logging in, 401
        this.webTestClient
                .post().uri("/api/resources/versions/")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void store_ValidRequest_201() {
        // has a record that the user owns
        Record record = TestHelper.mockRecord();
        record.setOwnerID(UUID.fromString(userID));
        record.setOwnerType(Record.OwnerType.User);
        repository.saveAndFlush(record);

        // dto of a new version
        VersionDTO dto = new VersionDTO();
        dto.setRecord(record.getId().toString());
        dto.setSchema("igsn-descriptive-v1");
        dto.setContent(Base64.encodeBase64String("some-string".getBytes()));

        // when POST, expects 201 with a header
        this.webTestClient
                .post().uri("/api/resources/versions/")
                .header("Authorization", getBasicAuthenticationHeader(username, password))
                .body(Mono.just(dto), VersionDTO.class)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("Location")
                .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.createdAt").exists();
    }

    @Test
    void store_ImportPermission_201OverwriteData() throws ParseException {
        // has a record that the user owns
        Record record = TestHelper.mockRecord();
        record.setOwnerID(UUID.fromString(userID));
        record.setOwnerType(Record.OwnerType.User);
        repository.saveAndFlush(record);

        // dto of a new version
        VersionDTO dto = new VersionDTO();
        dto.setRecord(record.getId().toString());
        dto.setSchema("igsn-descriptive-v1");
        dto.setContent(Base64.encodeBase64String("stuff".getBytes()));
        Date expectedDate = new SimpleDateFormat("dd/MM/yyyy").parse("02/02/1989");
        dto.setCreatedAt(expectedDate);

        // when POST, expects 201 with a Location header, overwritten createdAt
        this.webTestClient
                .post().uri("/api/resources/versions/")
                .header("Authorization", getBasicAuthenticationHeader(username, password))
                .body(Mono.just(dto), VersionDTO.class)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("Location")
                .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.createdAt", sameDay(expectedDate)).hasJsonPath();
    }
}