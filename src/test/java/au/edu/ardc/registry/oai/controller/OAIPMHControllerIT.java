package au.edu.ardc.registry.oai.controller;

import au.edu.ardc.registry.TestHelper;
import au.edu.ardc.registry.WebIntegrationTest;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.common.repository.RecordRepository;
import au.edu.ardc.registry.common.repository.VersionRepository;
import au.edu.ardc.registry.common.service.RecordService;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.service.VersionService;
import au.edu.ardc.registry.common.util.Helpers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;

public class OAIPMHControllerIT extends WebIntegrationTest {

	@Autowired
	RecordRepository recordRepository;

	@Autowired
	VersionRepository versionRepository;

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
	void handle_verb_GetRecord_no_identifier_returns() {
		this.webTestClient.get().uri(base_url + "?verb=GetRecord&metadataPrefix=ardc-igsn-desc-1.0").exchange()
				.expectStatus().isOk().expectBody().xpath("/OAI-PMH/error").exists();
	}

	@Test
	@DisplayName("Handles verb GetRecord with a non existent record identifier")
	void handle_verb_GetRecord_noid() throws Exception {
		this.webTestClient.get()
				.uri(base_url + "?verb=GetRecord&identifier=gjdhgjdfhgjfd&metadataPrefix=ardc-igsn-desc-1.0").exchange()
				.expectStatus().isOk().expectBody().xpath("/OAI-PMH/error")
				.isEqualTo("The value of the identifier argument is unknown or illegal in this repository.");
	}

	@Test
	void handle_verb_GetRecord_returnsRecord() throws IOException {
		Record record = TestHelper.mockRecord();
		recordRepository.saveAndFlush(record);
		String id = record.getId().toString();

		Version version = TestHelper.mockVersion(record);
		version.setCurrent(true);
		String validXML = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		version.setContent(validXML.getBytes());
		version.setSchema(SchemaService.ARDCv1);
		versionRepository.saveAndFlush(version);

		this.webTestClient.get()
				.uri(base_url + "?verb=GetRecord&identifier="+id+"&metadataPrefix="+SchemaService.ARDCv1).exchange()
				.expectStatus().isOk().expectBody()
				.xpath("/OAI-PMH/GetRecord/record").exists()
				.xpath("/OAI-PMH/GetRecord/record/header").exists()
				.xpath("/OAI-PMH/GetRecord/record/metadata/resources").exists()
				.xpath("/OAI-PMH/GetRecord/record/header/identifier").isEqualTo(id);
	}
}
