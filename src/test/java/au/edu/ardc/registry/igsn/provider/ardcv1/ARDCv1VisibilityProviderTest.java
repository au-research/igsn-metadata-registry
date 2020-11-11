package au.edu.ardc.registry.igsn.provider.ardcv1;

import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.provider.Metadata;
import au.edu.ardc.registry.common.provider.MetadataProviderFactory;
import au.edu.ardc.registry.common.provider.VisibilityProvider;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.util.Helpers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { SchemaService.class })
public class ARDCv1VisibilityProviderTest {

	@Autowired
	SchemaService service;

	@Test
	@DisplayName("Get Visibility = true from a record")
	void getVisibilityFromARDCV1_true() throws IOException {
		Schema schema = service.getSchemaByID(SchemaService.ARDCv1);
		String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		VisibilityProvider provider = (VisibilityProvider) MetadataProviderFactory.create(schema, Metadata.Visibility);
		boolean isVisible = provider.get(xml);
		assertTrue(isVisible);
	}

	@Test
	@DisplayName("Get Visibility = false from a record")
	void getVisibilityFromARDCV1_false() throws IOException {
		Schema schema = service.getSchemaByID(SchemaService.ARDCv1);
		String xml = Helpers.readFile("src/test/resources/xml/jsonld_input_test.xml");
		VisibilityProvider provider = (VisibilityProvider) MetadataProviderFactory.create(schema, Metadata.Visibility);
		boolean isVisible = provider.get(xml);
		assertFalse(isVisible);
	}

	@Test
	@DisplayName("Get Visibility from a record if 'isVisible' not present")
	void getVisibilityFromARDCV1_missing_element() throws IOException {
		Schema schema = service.getSchemaByID(SchemaService.ARDCv1);
		String xml = Helpers.readFile("src/test/resources/xml/invalid_sample_igsn_csiro_v3.xml");
		VisibilityProvider provider = (VisibilityProvider) MetadataProviderFactory.create(schema, Metadata.Visibility);
		boolean isVisible = provider.get(xml);
		assertFalse(isVisible);
	}

}