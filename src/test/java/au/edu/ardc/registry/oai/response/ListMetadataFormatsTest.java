package au.edu.ardc.registry.oai.response;

import au.edu.ardc.registry.common.config.ApplicationProperties;
import au.edu.ardc.registry.common.service.RecordService;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.service.VersionService;
import au.edu.ardc.registry.oai.model.ListMetadataFormatsFragment;
import au.edu.ardc.registry.oai.service.OAIPMHService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { OAIPMHService.class, ApplicationProperties.class, SchemaService.class })
@TestPropertySource(properties="app.oai.enabled=true")
public class ListMetadataFormatsTest {

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
	void listMetadataFormats() {
		String metadataPrefix = "ardc-igsn-desc-1.0";
		String schema = "https://identifiers.ardc.edu.au/igsn-schema/description/1.0/resource.xsd";
		String metadataNamespace = "https://identifiers.ardc.edu.au/schemas/ardc-igsn-desc";

		OAIListMetadataFormatsResponse response = (OAIListMetadataFormatsResponse) service.listMetadataFormats();
		assertThat(response).isInstanceOf(OAIResponse.class);

		ListMetadataFormatsFragment listMetadataFormatsFragment = new ListMetadataFormatsFragment();
		listMetadataFormatsFragment.setMetadataFormat(metadataPrefix, schema, metadataNamespace);

		response.setListMetadataFormatsFragment(listMetadataFormatsFragment);

		assertThat(response.getListMetadataFormatsFragment().getMetadataFormat()).isNotEmpty();
		assertThat(response.getListMetadataFormatsFragment().getMetadataFormat().get(0).getMetadataPrefix())
				.isEqualTo(metadataPrefix);
		assertThat(response.getListMetadataFormatsFragment().getMetadataFormat().get(0).getSchema()).isEqualTo(schema);
		assertThat(response.getListMetadataFormatsFragment().getMetadataFormat().get(0).getMetadataNamespace())
				.isEqualTo(metadataNamespace);

	}

}
