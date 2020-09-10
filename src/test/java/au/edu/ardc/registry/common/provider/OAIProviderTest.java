package au.edu.ardc.registry.common.provider;

import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.service.SchemaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { SchemaService.class })
public class OAIProviderTest {

	@Autowired
	SchemaService schemaService;

	@Test
	void create_Schema_ReturnsInstanceOfOAIProvider() {
		OAIProvider actual = (OAIProvider) MetadataProviderFactory
				.create(schemaService.getSchemaByID(SchemaService.ARDCv1), Metadata.OAI);
		assertThat(actual).isInstanceOf(OAIProvider.class);
	}

	@Test
	void getNameSpace() {
		Schema schema = schemaService.getSchemaByID(SchemaService.ARDCv1);
		String nameSpace = "https://identifiers.ardc.edu.au/schemas/ardc-igsn-desc";
		OAIProvider actual = (OAIProvider) MetadataProviderFactory
				.create(schemaService.getSchemaByID(SchemaService.ARDCv1), Metadata.OAI);
		assertThat(actual.getNamespace(schema)).isEqualTo(nameSpace);
	}

}
