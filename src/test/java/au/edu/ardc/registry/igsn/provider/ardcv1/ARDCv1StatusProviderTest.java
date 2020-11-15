package au.edu.ardc.registry.igsn.provider.ardcv1;

import au.edu.ardc.registry.TestHelper;
import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.provider.Metadata;
import au.edu.ardc.registry.common.provider.MetadataProviderFactory;
import au.edu.ardc.registry.common.provider.StatusProvider;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.util.Helpers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { SchemaService.class })
class ARDCv1StatusProviderTest {

    @Autowired
    SchemaService service;

	@Test
	void getStatus() throws IOException {
        Schema schema = service.getSchemaByID(SchemaService.ARDCv1);
        String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");

        Record record = TestHelper.mockRecord(UUID.randomUUID());
        Version version = TestHelper.mockVersion(record);
        version.setContent(xml.getBytes());
        version.setCurrent(true);
        version.setSchema(SchemaService.ARDCv1);
        record.setCurrentVersions(Arrays.asList(version));

        StatusProvider provider = (StatusProvider) MetadataProviderFactory.create(schema, Metadata.Status);
        String status = provider.get(record);
        assertThat(status).isEqualTo("Registered");
	}

    @Test
    void getStatus_Reserved() throws IOException {
        Schema schema = service.getSchemaByID(SchemaService.ARDCv1);
        String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");

        Record record = TestHelper.mockRecord(UUID.randomUUID());
        Version version = TestHelper.mockVersion(record);
        version.setContent(xml.getBytes());
        version.setCurrent(true);
        version.setSchema(SchemaService.ARDCv1);
        record.setCurrentVersions(Collections.singletonList(version));

        Identifier identifier = TestHelper.mockIdentifier(record);
        identifier.setStatus(Identifier.Status.RESERVED);
        record.setIdentifiers(Collections.singletonList(identifier));

        StatusProvider provider = (StatusProvider) MetadataProviderFactory.create(schema, Metadata.Status);
        String status = provider.get(record);
        assertThat(status).isEqualTo("Reserved");
    }
}