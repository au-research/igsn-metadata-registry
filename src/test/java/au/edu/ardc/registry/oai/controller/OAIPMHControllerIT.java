package au.edu.ardc.registry.oai.controller;

import au.edu.ardc.registry.TestHelper;
import au.edu.ardc.registry.WebIntegrationTest;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.common.repository.RecordRepository;
import au.edu.ardc.registry.common.repository.VersionRepository;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.oai.service.OAIPMHService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Date;

public class OAIPMHControllerIT extends WebIntegrationTest {

	@Autowired
	RecordRepository recordRepository;

	@Autowired
	VersionRepository versionRepository;

	@Autowired
	OAIPMHService service;

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
				.uri(base_url + "?verb=GetRecord&identifier=" + id + "&metadataPrefix=" + SchemaService.ARDCv1)
				.exchange().expectStatus().isOk().expectBody().xpath("/OAI-PMH/GetRecord/record").exists()
				.xpath("/OAI-PMH/GetRecord/record/header").exists()
				.xpath("/OAI-PMH/GetRecord/record/metadata/resources").exists()
				.xpath("/OAI-PMH/GetRecord/record/header/identifier").isEqualTo(id);
	}

	@Test
	void handle_verb_ListRecords_returnsRecords() throws IOException {
		int i = 0;
		Date versionDate = service.convertDate("2020-09-23T09:30:25Z");
		for (i = 0; i < 150; i++) {
			Record record = TestHelper.mockRecord();
			record.setModifiedAt(versionDate);
			recordRepository.saveAndFlush(record);

			Version version = TestHelper.mockVersion(record);
			version.setCurrent(true);
			String validXML = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
			version.setContent(validXML.getBytes());
			version.setSchema(SchemaService.ARDCv1);
			version.setCreatedAt(versionDate);
			versionRepository.saveAndFlush(version);
		}

		String from = "2020-09-23T09:30:23Z";
		String until = "2020-09-23T09:30:28Z";
		this.webTestClient.get()
				.uri(base_url + "?verb=ListRecords&metadataPrefix=" + SchemaService.ARDCv1 + "&from=" + from + "&until="
						+ until)
				.exchange().expectStatus().isOk().expectBody().xpath("/OAI-PMH/ListRecords").exists()
				.xpath("/OAI-PMH/ListRecords/record/header").exists()
				.xpath("/OAI-PMH/ListRecords/record/metadata/resources").exists();
	}

	@Test
	void handle_verb_ListRecords_returnsNoRecordsMatchException() throws IOException {
		int i = 0;
		Date versionDate = service.convertDate("2020-09-23T09:30:25Z");
		for (i = 0; i < 150; i++) {
			Record record = TestHelper.mockRecord();
			record.setModifiedAt(versionDate);
			recordRepository.saveAndFlush(record);

			Version version = TestHelper.mockVersion(record);
			version.setCurrent(true);
			String validXML = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
			version.setContent(validXML.getBytes());
			version.setSchema(SchemaService.ARDCv1);
			version.setCreatedAt(versionDate);
			versionRepository.saveAndFlush(version);
		}

		String from = "2020-09-23T09:30:28Z";
		String until = "2020-09-23T09:30:22Z";
		this.webTestClient.get()
				.uri(base_url + "?verb=ListRecords&metadataPrefix=" + SchemaService.ARDCv1 + "&from=" + from + "&until="
						+ until)
				.exchange().expectStatus().isOk().expectBody().xpath("/OAI-PMH/error").exists()
				.xpath("/OAI-PMH/error[@code='noRecordsMatch']").exists();
	}

}
