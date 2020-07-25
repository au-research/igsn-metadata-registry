package au.edu.ardc.igsn.controller.api;


import au.edu.ardc.igsn.IGSNMetadataRegistry;
import au.edu.ardc.igsn.TestHelper;
import au.edu.ardc.igsn.dto.RecordDTO;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.repository.RecordRepository;
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
import reactor.core.publisher.Mono;

import javax.xml.bind.DatatypeConverter;
import java.net.URISyntaxException;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {IGSNMetadataRegistry.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIf(expression = "${keycloak.enabled}", reason = "Disable test if keycloak is not enabled", loadContext = true)
@ActiveProfiles("integration")
@AutoConfigureWebTestClient
public class RecordResourceControllerIT {

    @Autowired
    RecordRepository repository;

    @Value("${test.kc.user.username}")
    private String username;

    @Value("${test.kc.user.password}")
    private String password;

    @Value("${test.kc.user.rsid}")
    private String resourceID;

    @Autowired
    private WebTestClient webTestClient;


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
    void create_InsufficientPermission_403() throws URISyntaxException {
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
                .jsonPath("$.id").exists();
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

    // todo update_InsufficientPermission_403
    // todo update_SufficientPermission_202
    // todo delete_NotLoggedIn_401
    // todo delete_InsufficientPermission_403
    // todo delete_SufficientPermission_202

    private String getBasicAuthenticationHeader(String username, String password) {
        return "Basic " + DatatypeConverter.printBase64Binary((username + ":" + password).getBytes());
    }
}