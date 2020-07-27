package au.edu.ardc.igsn.controller.api;


import au.edu.ardc.igsn.KeycloakIntegrationTest;
import au.edu.ardc.igsn.TestHelper;
import au.edu.ardc.igsn.dto.RecordDTO;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.repository.RecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static org.exparity.hamcrest.date.DateMatchers.sameDay;

class RecordResourceControllerIT extends KeycloakIntegrationTest {

    @Autowired
    RecordRepository repository;

    @Test
    void index_NotLoggedIn_401() {
        this.webTestClient
                .get().uri("/api/resources/records/")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // todo index_hasRecords_showRecords()
    // todo index_hasManyRecords_showPagination()

    @Test
    void show_NotLoggedIn_401() {
        // given a record
        Record record = TestHelper.mockRecord();
        repository.saveAndFlush(record);

        // when show without permission, get 401
        this.webTestClient
                .get().uri(String.format("/api/resources/records/%s", record.getId()))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void show_recordDoesNotExist_404() {
        this.webTestClient
                .get().uri(String.format("/api/resources/records/%s", UUID.randomUUID()))
                .header("Authorization", getBasicAuthenticationHeader(username, password))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void show_recordExist_200() {
        // given a record
        Record record = TestHelper.mockRecord();
        repository.saveAndFlush(record);

        // when show with authentication, gets 200 and the record
        this.webTestClient
                .get().uri(String.format("/api/resources/records/%s", record.getId()))
                .header("Authorization", getBasicAuthenticationHeader(username, password))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(record.getId().toString())
                .jsonPath("$.status").isEqualTo(record.getStatus().toString())
                .jsonPath("$.createdAt").exists();
    }

    @Test
    void create_NotLoggedIn_401() {
        // when POST without logging in, 401
        this.webTestClient
                .post().uri("/api/resources/records/")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void create_InsufficientPermission_403() {
        // the request is for a different allocation that the user did not have access to
        RecordDTO requestDTO = new RecordDTO();
        requestDTO.setAllocationID(UUID.randomUUID());

        // when POST, expects 403
        this.webTestClient
                .post().uri("/api/resources/records/")
                .header("Authorization", getBasicAuthenticationHeader(username, password))
                .body(Mono.just(requestDTO), RecordDTO.class)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void create_SufficientPermission_201WithLocation() {
        // request a record with the associated allocation
        RecordDTO requestDTO = new RecordDTO();
        requestDTO.setAllocationID(UUID.fromString(resourceID));

        // when POST, expects 201, Location Header, and the record ID in the body
        this.webTestClient
                .post().uri("/api/resources/records/")
                .header("Authorization", getBasicAuthenticationHeader(username, password))
                .body(Mono.just(requestDTO), RecordDTO.class)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("Location")
                .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.createdAt").exists();
    }

    @Test
    void create_ImportPermission_201OverwriteData() throws ParseException {
        // this test requires the provided credentials have igsn:import scope against the allocation

        // request a record with the associated allocation, with the overwritten createdAt date
        RecordDTO requestDTO = new RecordDTO();
        requestDTO.setAllocationID(UUID.fromString(resourceID));
        Date expectedDate = new SimpleDateFormat("dd/MM/yyyy").parse("02/02/1989");
        requestDTO.setCreatedAt(expectedDate);

        // when POST, expects 201, Location Header, and the record ID in the body
        this.webTestClient
                .post().uri("/api/resources/records/")
                .header("Authorization", getBasicAuthenticationHeader(username, password))
                .body(Mono.just(requestDTO), RecordDTO.class)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("Location")
                .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.createdAt").exists()
                .jsonPath("$.createdAt", sameDay(expectedDate)).hasJsonPath();
    }

    @Test
    void update_NotLoggedIn_401() {
        // given a record
        Record record = TestHelper.mockRecord();
        repository.saveAndFlush(record);

        // when PUT without logged in, 401
        this.webTestClient
                .put().uri("/api/resources/records/" + record.getId().toString())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void update_RecordNotFound_404() {
        // the request is to update the creatorID
        RecordDTO requestDTO = new RecordDTO();

        // when PUT without logged in, 401
        this.webTestClient
                .put().uri("/api/resources/records/" + UUID.randomUUID().toString())
                .header("Authorization", getBasicAuthenticationHeader(username, password))
                .body(Mono.just(requestDTO), RecordDTO.class)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void update_InsufficientPermission_403() {
        // given a record with a random Allocation
        Record record = TestHelper.mockRecord();
        record.setAllocationID(UUID.randomUUID());
        repository.saveAndFlush(record);

        // the request is to update the creatorID
        RecordDTO requestDTO = new RecordDTO();
        requestDTO.setCreatorID(UUID.randomUUID());

        // when PUT with default credentials (does not have access to that allocation), 403
        // todo refactor update will affect this
        this.webTestClient
                .put().uri("/api/resources/records/" + record.getId().toString())
                .header("Authorization", getBasicAuthenticationHeader(username, password))
                .body(Mono.just(requestDTO), RecordDTO.class)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void update_SufficientPermission_202() {
        // given a record with a the same allocation as the credentials
        Record record = TestHelper.mockRecord();
        record.setAllocationID(UUID.fromString(resourceID));
        record.setStatus(Record.Status.DRAFT);
        record.setOwnerType(Record.OwnerType.User);
        record.setOwnerID(UUID.fromString(userID));
        repository.saveAndFlush(record);

        // the request is to update the status
        RecordDTO requestDTO = new RecordDTO();
        requestDTO.setStatus(Record.Status.PUBLISHED);

        // when PUT with default credentials, 202, status is updated
        this.webTestClient
                .put().uri("/api/resources/records/" + record.getId().toString())
                .header("Authorization", getBasicAuthenticationHeader(username, password))
                .body(Mono.just(requestDTO), RecordDTO.class)
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .jsonPath("$.id").isEqualTo(record.getId().toString())
                .jsonPath("$.status").isEqualTo(Record.Status.PUBLISHED.toString());
    }

    @Test
    void delete_NotLoggedIn_401() {
        // given a record
        Record record = TestHelper.mockRecord();
        repository.saveAndFlush(record);

        // when DELETE without logging in, 401
        this.webTestClient
                .post().uri("/api/resources/records/" + record.getId().toString())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void delete_RecordNotFound_404() {
        // when DELETE with credentials on a non existence record, expects 404
        this.webTestClient
                .delete().uri("/api/resources/records/" + UUID.randomUUID().toString())
                .header("Authorization", getBasicAuthenticationHeader(username, password))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void delete_InsufficientPermission_403() {
        // given a record
        Record record = TestHelper.mockRecord();
        record.setOwnerID(UUID.randomUUID());
        record.setOwnerType(Record.OwnerType.User);
        repository.saveAndFlush(record);

        // when DELETE with credentials, expects 403
        this.webTestClient
                .delete().uri("/api/resources/records/" + record.getId().toString())
                .header("Authorization", getBasicAuthenticationHeader(username, password))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void delete_SufficientPermission_202() {
        // given a record owned by the user
        Record record = TestHelper.mockRecord();
        record.setOwnerType(Record.OwnerType.User);
        record.setOwnerID(UUID.fromString(userID));
        repository.saveAndFlush(record);

        // when DELETE with credentials, expects 202
        this.webTestClient
                .delete().uri("/api/resources/records/" + record.getId().toString())
                .header("Authorization", getBasicAuthenticationHeader(username, password))
                .exchange()
                .expectStatus().isAccepted();
    }
}