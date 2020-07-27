package au.edu.ardc.igsn;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.xml.bind.DatatypeConverter;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIf(expression = "${keycloak.enabled}", reason = "Disable test if keycloak is not enabled", loadContext = true)
@ActiveProfiles("integration")
@AutoConfigureWebTestClient
public class KeycloakIntegrationTest {

    @Value("${test.kc.user.id}")
    public String userID;

    @Value("${test.kc.user.username}")
    public String username;

    @Value("${test.kc.user.password}")
    public String password;

    @Value("${test.kc.user.rsid}")
    public String resourceID;

    @Autowired
    public WebTestClient webTestClient;

    public String getBasicAuthenticationHeader(String username, String password) {
        return "Basic " + DatatypeConverter.printBase64Binary((username + ":" + password).getBytes());
    }
}
