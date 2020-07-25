package au.edu.ardc.igsn.controller.api;


import au.edu.ardc.igsn.IGSNMetadataRegistry;
import au.edu.ardc.igsn.TestHelper;
import au.edu.ardc.igsn.dto.RecordDTO;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.repository.RecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.xml.bind.DatatypeConverter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {IGSNMetadataRegistry.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIf(expression = "${keycloak.enabled}", reason = "Disable test if keycloak is not enabled", loadContext = true)
@ActiveProfiles("integration")
public class RecordResourceControllerIT {

    @LocalServerPort
    int localPort;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    RecordRepository repository;

    @Value("${test.kc.user.username}")
    private String username;

    @Value("${test.kc.user.password}")
    private String password;

    private String baseUrl;

    @BeforeEach
    public void setup() {
        baseUrl = "http://localhost:" + localPort + "/api/resources/records/";
    }

    @Test
    void index_NotLoggedIn_401() throws URISyntaxException {
        URI uri = new URI(baseUrl);
        ResponseEntity<?> result = restTemplate.getForEntity(uri, Object.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // todo index_hasRecords_showRecords()
    // todo index_hasManyRecords_showPagination()

    @Test
    void show_NotLoggedIn_401() throws URISyntaxException {
        // given a record
        Record record = TestHelper.mockRecord();
        repository.saveAndFlush(record);

        // when show without permission
        URI uri = new URI(baseUrl + record.getId().toString());
        ResponseEntity<?> result = restTemplate.getForEntity(uri, Object.class);

        // 401
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void show_recordDoesNotExist_404() throws URISyntaxException {
        HttpHeaders requestHeaders = getAuthenticatedHeader(username, password);
        HttpEntity<?> requestEntity = new HttpEntity<>(requestHeaders);

        // when show with authentication
        URI uri = new URI(baseUrl + UUID.randomUUID().toString());
        ResponseEntity<?> result = restTemplate.exchange(uri, HttpMethod.GET, requestEntity, Object.class);

        // 404
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void show_recordExist_200() throws URISyntaxException {
        // given a record
        Record expected = TestHelper.mockRecord();
        repository.saveAndFlush(expected);

        HttpHeaders requestHeaders = getAuthenticatedHeader(username, password);
        HttpEntity<?> requestEntity = new HttpEntity<>(requestHeaders);

        // when show with authentication
        URI uri = new URI(baseUrl + expected.getId().toString());
        ResponseEntity<RecordDTO> result = restTemplate.exchange(uri, HttpMethod.GET, requestEntity, RecordDTO.class);

        // 200
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);

        // result is a valid recordDTO that matches the initial record
        RecordDTO actual = result.getBody();
        assertThat(actual).isNotNull();
        assertThat(actual).isInstanceOf(RecordDTO.class);
        assertThat(actual.getId()).isEqualTo(expected.getId());
        assertThat(actual.getStatus()).isEqualTo(expected.getStatus());
        assertThat(actual.getCreatedAt()).isEqualTo(expected.getCreatedAt());
    }

    @Test
    void create_NotLoggedIn_401() throws URISyntaxException {
        // when POST without logged in
        URI uri = new URI(baseUrl);
        HttpEntity<?> requestEntity = new HttpEntity<>(new HttpHeaders());
        ResponseEntity<?> result = restTemplate.exchange(uri, HttpMethod.POST, requestEntity, Object.class);

        // 401
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void create_InsufficientPermission_403() throws URISyntaxException {
        // the request is for a different allocation that the user did not have access to
        RecordDTO requestDTO = new RecordDTO();
        requestDTO.setAllocationID(UUID.randomUUID());
        HttpEntity<?> requestEntity = new HttpEntity<>(
                requestDTO, getAuthenticatedHeader(username, password)
        );

        // when POST
        URI uri = new URI(baseUrl);
        ResponseEntity<?> result = restTemplate.exchange(uri, HttpMethod.POST, requestEntity, Object.class);

        // get 403
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // todo create_SufficientPermission_201WithLocation
    // todo update_NotLoggedIn_401
    // todo update_InsufficientPermission_403
    // todo update_SufficientPermission_202
    // todo delete_NotLoggedIn_401
    // todo delete_InsufficientPermission_403
    // todo delete_SufficientPermission_202

    private HttpHeaders getAuthenticatedHeader(String username, String password) {
        String authorizationHeader = "Basic " + DatatypeConverter.printBase64Binary((username + ":" + password).getBytes());
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        requestHeaders.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        requestHeaders.add("Authorization", authorizationHeader);
        return requestHeaders;
    }
}