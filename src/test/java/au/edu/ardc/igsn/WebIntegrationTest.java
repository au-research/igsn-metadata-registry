package au.edu.ardc.igsn;

import au.edu.ardc.igsn.repository.IdentifierRepository;
import au.edu.ardc.igsn.repository.RecordRepository;
import au.edu.ardc.igsn.repository.URLRepository;
import au.edu.ardc.igsn.repository.VersionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.xml.bind.DatatypeConverter;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "10000")
@ActiveProfiles("integration")
public abstract class WebIntegrationTest {

    @Autowired
    public WebTestClient webTestClient;

    @Autowired
    private IdentifierRepository identifierRepository;

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private VersionRepository versionRepository;

    @Autowired
    private URLRepository urlRepository;

    public String getBasicAuthenticationHeader(String username, String password) {
        return "Basic " + DatatypeConverter.printBase64Binary((username + ":" + password).getBytes());
    }

    @AfterEach
    void cleanUp() {
        urlRepository.deleteAll();
        versionRepository.deleteAll();
        identifierRepository.deleteAll();
        recordRepository.deleteAll();
    }

}
