package au.edu.ardc.igsn;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.xml.bind.DatatypeConverter;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public abstract class WebIntegrationTest {

    @Autowired
    public WebTestClient webTestClient;

    public String getBasicAuthenticationHeader(String username, String password) {
        return "Basic " + DatatypeConverter.printBase64Binary((username + ":" + password).getBytes());
    }

}
