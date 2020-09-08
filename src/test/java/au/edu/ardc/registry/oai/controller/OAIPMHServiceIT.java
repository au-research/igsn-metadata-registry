package au.edu.ardc.registry.oai.controller;

import au.edu.ardc.registry.WebIntegrationTest;
import au.edu.ardc.registry.common.service.RecordService;
import au.edu.ardc.registry.common.service.VersionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class OAIPMHServiceIT extends WebIntegrationTest {

	@Autowired
	RecordService recordService;

	@Autowired
	VersionService versionService;

	final String base_url = "/api/services/oai-pmh";

	@Test
	void invalid_verb_return_error() throws Exception {
		System.out.print("in test");

	}

	@Test
	@DisplayName("Utilises SchemaServices to obtain a list of approved metadataFormats for OAIPMH")
	void handle_verb_ListMetadataFormats_returns() throws Exception {
		this.webTestClient.get().uri(base_url + "?verb=ListMetadataFormats").exchange().expectStatus().isOk()
				.expectBody().xpath("/OAI-PMH/ListMetadataFormats/metadataFormat/schema")
				.isEqualTo("ARDC IGSN Descriptive v1.0");
	}

}
