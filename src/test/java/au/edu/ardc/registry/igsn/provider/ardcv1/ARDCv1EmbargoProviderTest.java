package au.edu.ardc.registry.igsn.provider.ardcv1;

import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.provider.EmbargoEndProvider;
import au.edu.ardc.registry.common.provider.Metadata;
import au.edu.ardc.registry.common.provider.MetadataProviderFactory;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.exception.ContentProviderNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { SchemaService.class })
public class ARDCv1EmbargoProviderTest {

	@Autowired
	SchemaService service;

	@Test
	@DisplayName("Get embargoEnd of a ARDCV1 record")
	void extractEmbargoEndFromARDCV1() throws IOException {
		Schema schema = service.getSchemaByID(SchemaService.ARDCv1);
		String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1_embargoEndPast.xml");

		EmbargoEndProvider provider = (EmbargoEndProvider) MetadataProviderFactory.create(schema, Metadata.EmbargoEnd);
		Date embargoEnd = provider.get(xml);
		assertEquals(embargoEnd, Helpers.convertDate("2020-09-07"));
	}

	@Test
	@DisplayName("Attempt to get embargoEnd of a ARDCV1 record with no embargoEnd attribute")
	void extractEmbargoEndFromARDCV1NoEmbargoEnd() throws IOException {
		Schema schema = service.getSchemaByID(SchemaService.ARDCv1);
		String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");

		EmbargoEndProvider provider = (EmbargoEndProvider) MetadataProviderFactory.create(schema, Metadata.EmbargoEnd);
		Date embargoEnd = provider.get(xml);
		assertEquals(embargoEnd, null);
	}

	@Test
	@DisplayName("Attempt to get embargoEnd of a ARDCV1 record with a YYYY date format")
	void extractEmbargoEndFromARDCV1EmbargoEndYYYY() throws IOException {
		Schema schema = service.getSchemaByID(SchemaService.ARDCv1);
		String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1_EmbargoEndYYYY.xml");

		EmbargoEndProvider provider = (EmbargoEndProvider) MetadataProviderFactory.create(schema, Metadata.EmbargoEnd);
		Date embargoEnd = provider.get(xml);
		assertEquals(embargoEnd, Helpers.convertDate("2021"));
	}

	@Test
	@DisplayName("Attempt to get embargoEnd of a oai_dc")
	void extractEmbargoEndFromARDCV1Invalid() throws IOException {
		Schema schema = service.getSchemaByID(SchemaService.OAIDC);
		try {
			EmbargoEndProvider pro = (EmbargoEndProvider) MetadataProviderFactory.create(schema, Metadata.EmbargoEnd);
		} catch (ContentProviderNotFoundException ex){
			assertEquals(ex.getMessageID(),"api.error.content_not_supported");
		}
	}
}
