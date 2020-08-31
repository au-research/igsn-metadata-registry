package au.edu.ardc.registry.oai.controller;

import au.edu.ardc.registry.WebIntegrationTest;
import au.edu.ardc.registry.common.service.RecordService;
import au.edu.ardc.registry.common.service.VersionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
