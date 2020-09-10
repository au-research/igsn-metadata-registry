package au.edu.ardc.registry.igsn.controller;

import au.edu.ardc.registry.TestHelper;
import au.edu.ardc.registry.WebIntegrationTest;
import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.common.repository.IdentifierRepository;
import au.edu.ardc.registry.common.repository.RecordRepository;
import au.edu.ardc.registry.common.repository.VersionRepository;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.util.Helpers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.UUID;

class IGSNDescriptionPublicControllerIT extends WebIntegrationTest {

	final String baseUrl = "/api/public/igsn-description/";

	@Autowired
	RecordRepository recordRepository;

	@Autowired
	VersionRepository versionRepository;

	@Autowired
	IdentifierRepository identifierRepository;

	@Test
	void index_validRequest_returnsXML() throws IOException {
		// given a record, a version and an identifier
		Record record = TestHelper.mockRecord();
		recordRepository.saveAndFlush(record);
		Version version = TestHelper.mockVersion(record);
		version.setCurrent(true);
		version.setSchema(SchemaService.ARDCv1);
		String validXML = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		version.setContent(validXML.getBytes());
		versionRepository.saveAndFlush(version);
		Identifier identifier = TestHelper.mockIdentifier(record);
		identifier.setType(Identifier.Type.IGSN);
		identifier.setValue("10273/XXAA");
		identifierRepository.saveAndFlush(identifier);

		// when get igsn-description, returns the version content
		this.webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(baseUrl).queryParam("identifier", "10273/XXAA").build()).exchange()
				.expectStatus().isOk().expectHeader().contentType(MediaType.APPLICATION_XML).expectBody().xml(validXML);
	}

	@Test
	void index_notFoundIdentifier_404() {
		this.webTestClient.get().uri(
				uriBuilder -> uriBuilder.path(baseUrl).queryParam("identifier", UUID.randomUUID().toString()).build())
				.exchange().expectStatus().isNotFound();
	}

	@Test
	void index_notFoundVersion_404() {
		// given a record, an identifier and no version
		Record record = TestHelper.mockRecord();
		recordRepository.saveAndFlush(record);
		Identifier identifier = TestHelper.mockIdentifier(record);
		identifier.setType(Identifier.Type.IGSN);
		identifier.setValue("10273/XXAB");
		identifierRepository.saveAndFlush(identifier);

		// not found
		this.webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(baseUrl).queryParam("identifier", "10273/XXAB").build()).exchange()
				.expectStatus().isNotFound();

		// a version with the wrong schema
		Version version = TestHelper.mockVersion(record);
		version.setCurrent(true);
		version.setSchema("wrong-schema");
		version.setContent("stuff".getBytes());
		versionRepository.saveAndFlush(version);

		// not found
		this.webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(baseUrl).queryParam("identifier", "10273/XXAB").build()).exchange()
				.expectStatus().isNotFound();

		// right schema, not current
		Version version2 = TestHelper.mockVersion(record);
		version2.setCurrent(false);
		version2.setSchema(SchemaService.ARDCv1);
		version2.setContent("stuff".getBytes());
		versionRepository.saveAndFlush(version2);

		this.webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(baseUrl).queryParam("identifier", "10273/XXAB").build()).exchange()
				.expectStatus().isNotFound();
	}

}