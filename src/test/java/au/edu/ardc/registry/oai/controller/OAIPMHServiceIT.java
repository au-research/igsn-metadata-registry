package au.edu.ardc.registry.oai.controller;

import au.edu.ardc.registry.WebIntegrationTest;
import au.edu.ardc.registry.common.repository.RecordRepository;
import au.edu.ardc.registry.common.repository.VersionRepository;
import au.edu.ardc.registry.common.service.RecordService;
import au.edu.ardc.registry.common.service.VersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

public class OAIPMHServiceIT extends WebIntegrationTest {

	@Autowired
	RecordRepository recordRepository;

	@Autowired
	VersionRepository versionRepository;

	@MockBean
	RecordService recordService;

	@MockBean
	VersionService versionService;

	@BeforeEach
	void setUp() {
		versionRepository.flush();
		versionRepository.deleteAll();
		versionRepository.flush();

		recordRepository.flush();
		recordRepository.deleteAll();
		recordRepository.flush();
	}

	final String base_url = "/api/services/oai-pmh";

	@Test
	@DisplayName("Utilises SchemaServices to obtain a list of approved metadataFormats for OAIPMH")
	void handle_verb_ListMetadataFormats_returns() throws Exception {
		this.webTestClient.get().uri(base_url + "?verb=ListMetadataFormats").exchange().expectStatus().isOk()
				.expectBody().xpath("/OAI-PMH/ListMetadataFormats/metadataFormat/schema")
				.isEqualTo("https://identifiers.ardc.edu.au/igsn-schema/description/1.0/resource.xsd");
	}

	@Test
	@DisplayName("Handles verb GetRecord with a no record identifier")
	void handle_verb_GetRecord_no_identifier_returns() throws Exception {
		this.webTestClient.get().uri(base_url + "?verb=GetRecord&metadataPrefix=ardc-igsn-desc-1.0").exchange()
				.expectStatus().isOk().expectBody().xpath("/OAI-PMH/error").isEqualTo("Identifier required");
	}

	@Test
	@DisplayName("Handles verb GetRecord with a non existent record identifier")
	void handle_verb_GetRecord_returns() throws Exception {
		this.webTestClient.get()
				.uri(base_url + "?verb=GetRecord&identifier=gjdhgjdfhgjfd&metadataPrefix=ardc-igsn-desc-1.0").exchange()
				.expectStatus().isOk().expectBody().xpath("/OAI-PMH/error")
				.isEqualTo("Record with identifier does not exist");
	}

}
