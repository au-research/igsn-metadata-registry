package au.edu.ardc.igsn.controller.api.services;

import au.edu.ardc.igsn.WebIntegrationTest;
import au.edu.ardc.igsn.service.RecordService;
import au.edu.ardc.igsn.service.VersionService;
import org.aspectj.apache.bcel.util.ClassLoaderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

public class OAIPMHServiceIT extends WebIntegrationTest {

    @Autowired
    RecordService recordService;

    @Autowired
    VersionService versionService;

    final String baseUrl = "/api/services/oai-pmh";

    @Test
    void invalid_verb_return_error() throws Exception {
        System.out.print("in test");

    }

    @Test
    void valid_Identify_verb_return_identify() {
        this.webTestClient.get().uri(baseUrl + "?verb=Identify").exchange().expectStatus().isOk();
    }
}
