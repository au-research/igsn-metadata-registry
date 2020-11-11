package au.edu.ardc.registry.common.provider.text;

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

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { SchemaService.class })
class LineContentProviderTest {

    @Autowired
    SchemaService schemaService;

    @Test
    void get_all_identifiers() throws IOException {
        String text = Helpers.readFile("src/test/resources/data/igsn.txt");
        Schema schema = schemaService.getSchemaByID(SchemaService.IGSNList);
        IdentifierProvider provider = (IdentifierProvider) MetadataProviderFactory.create(schema, Metadata.Identifier);
        assert provider != null;
        List<String> identifiers = provider.getAll(text);
        assertEquals(4, identifiers.size());
    }


    @Test
    void test_get_identifier() throws IOException {
        /*

        10273/XXAB0011P
        10273/XXAB00312
        10273/XXAB00417
        10273/XXAB00728

         */
        String text = Helpers.readFile("src/test/resources/data/igsn.txt");
        Schema schema = schemaService.getSchemaByID(SchemaService.IGSNList);
        IdentifierProvider provider = (IdentifierProvider) MetadataProviderFactory.create(schema, Metadata.Identifier);
        assert provider != null;
        String identifier;
        identifier = provider.get(text, 4);
        assertNull(identifier);
        identifier = provider.get(text, -4);
        assertNull(identifier);
        identifier = provider.get(text, 0);
        assertEquals("10273/XXAB0011P", identifier);
        identifier = provider.get(text, 3);
        assertEquals("10273/XXAB00728", identifier);



    }

}