package au.edu.ardc.registry.common.provider;

import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.igsn.provider.ardcv1.ARDCv1IdentifierProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { SchemaService.class })
class MetadataProviderFactoryTest {

	@Autowired
	SchemaService schemaService;

	@Test
	void create_Schema_ReturnsInstanceOfIdentifierProvider() {

		IdentifierProvider actual = (IdentifierProvider) MetadataProviderFactory
				.create(schemaService.getSchemaByID(SchemaService.ARDCv1), Metadata.Identifier);

		assertThat(actual).isInstanceOf(IdentifierProvider.class);
		assertThat(actual).isInstanceOf(ARDCv1IdentifierProvider.class);
	}

}