package au.edu.ardc.registry.igsn.provider.csirov3;

import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.provider.IdentifierProvider;
import au.edu.ardc.registry.common.provider.Metadata;
import au.edu.ardc.registry.common.provider.MetadataProviderFactory;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.util.Helpers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { SchemaService.class })
public class CSIROv3IProviderTest {

    @Autowired
    SchemaService service;

    @Test
    public void extractIdentifierFromCSIROv3() throws Exception {
        Schema schema = service.getSchemaByID(SchemaService.CSIROv3);
        String xml = Helpers.readFile("src/test/resources/xml/sample_igsn_csiro_v3.xml");
        IdentifierProvider provider = (IdentifierProvider) MetadataProviderFactory.create(schema, Metadata.Identifier);
        provider.setPrefix("10273");
        String identifierValue = provider.get(xml);
        assertEquals("10273/CSTSTDOCO1", identifierValue);
        provider.setPrefix("10273/");
        identifierValue = provider.get(xml);
        assertEquals("10273/CSTSTDOCO1", identifierValue);
    }
}