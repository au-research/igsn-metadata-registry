package au.edu.ardc.igsn.controller.api;

import au.edu.ardc.igsn.IGSNMetadataRegistry;
import au.edu.ardc.igsn.User;
import org.junit.Before;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.xml.bind.DatatypeConverter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {IGSNMetadataRegistry.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIf(expression = "${keycloak.enabled}", reason = "Disable test if keycloak is not enabled", loadContext = true)
@ActiveProfiles("integration")
public class MeControllerIT {

    @LocalServerPort
    int localPort;

    @Autowired
    TestRestTemplate restTemplate;

    @Value("${test.kc.user.username}")
    private String username;

    @Value("${test.kc.user.password}")
    private String password;

    private String baseUrl;

    @BeforeEach
    public void setup() {
        baseUrl = "http://localhost:" + localPort + "/api/me/";
    }


    @Test
    public void non_logged_in_user_will_get_an_unauthorized() throws URISyntaxException {
        URI uri = new URI(baseUrl);
        ResponseEntity<?> result = restTemplate.getForEntity(uri, Object.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void logged_in_user_will_have_their_user_profile_shown() throws URISyntaxException {
        URI uri = new URI(baseUrl);
        String authorizationHeader = "Basic " + DatatypeConverter.printBase64Binary((username + ":" + password).getBytes());
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        requestHeaders.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        requestHeaders.add("Authorization", authorizationHeader);

        HttpEntity<?> requestEntity = new HttpEntity<>(requestHeaders);

        ResponseEntity<User> result = restTemplate.exchange(uri, HttpMethod.GET, requestEntity, User.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);

        User user = result.getBody();
        assert user != null;
        assertThat(user.getEmail()).isEqualTo(username);
    }
}