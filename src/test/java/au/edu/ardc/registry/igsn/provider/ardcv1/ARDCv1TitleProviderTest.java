package au.edu.ardc.registry.igsn.provider.ardcv1;

import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.provider.IdentifierProvider;
import au.edu.ardc.registry.common.provider.Metadata;
import au.edu.ardc.registry.common.provider.MetadataProviderFactory;
import au.edu.ardc.registry.common.provider.TitleProvider;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.util.Helpers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {SchemaService.class})
class ARDCv1TitleProviderTest {
    @Autowired
    SchemaService service;

    @Test
    void extractTitleFromARDCV1() throws IOException {
        Schema schema = service.getSchemaByID(SchemaService.ARDCv1);
        String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");

        TitleProvider provider = (TitleProvider) MetadataProviderFactory.create(schema, Metadata.Title);
        String identifierValue = provider.get(schema, xml);
        assertEquals(identifierValue, "This Tiltle also left blank on purpose");
    }
}