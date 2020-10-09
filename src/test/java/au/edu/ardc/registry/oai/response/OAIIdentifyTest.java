package au.edu.ardc.registry.oai.response;

import au.edu.ardc.registry.common.config.ApplicationProperties;
import au.edu.ardc.registry.common.service.RecordService;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.service.VersionService;
import au.edu.ardc.registry.oai.service.OAIPMHService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { OAIPMHService.class, ApplicationProperties.class, SchemaService.class })
public class OAIIdentifyTest {

    @Autowired
    OAIPMHService service;

    @MockBean
    VersionService versionService;

    @MockBean
    RecordService recordService;

    @Autowired
    ApplicationProperties applicationProperties;

    @Autowired
    SchemaService schemaService;

    @Test
    void identify() {
        OAIIdentifyResponse response = (OAIIdentifyResponse) service.identify();
        assertThat(response).isInstanceOf(OAIResponse.class);
        assertThat(response.getIdentify().getRepositoryName()).isEqualTo(applicationProperties.getName());
    }

}
