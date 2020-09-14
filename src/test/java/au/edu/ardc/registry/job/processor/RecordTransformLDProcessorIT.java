package au.edu.ardc.registry.job.processor;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class RecordTransformLDProcessorIT extends WebIntegrationTest {

	@Autowired
	VersionService versionService;

	@Autowired
	RecordService recordService;

	@Autowired
	SchemaService schemaService;

	@Autowired
	private RecordRepository recordRepository;

	@Autowired
	private VersionRepository versionRepository;

	@Test
	void process_createsNewVersion() throws Exception {

		// given a record with a current version with schema ardcv1
		Record record = TestHelper.mockRecord();
		recordRepository.saveAndFlush(record);

		Version version = TestHelper.mockVersion(record);
		String validXML = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		version.setCurrent(true);
		version.setContent(validXML.getBytes());
		version.setSchema(SchemaService.ARDCv1);
		versionRepository.saveAndFlush(version);

		// when process
		RecordTransformLDProcessor processor = new RecordTransformLDProcessor(versionService, recordService,
				schemaService);
		Record actual = processor.process(record);

		// a new version is created and it's json-ld with content
		assertThat(actual).isNotNull();
		assertThat(actual.getCurrentVersions()).hasSize(2);

		Version actualVersion = versionService.findVersionForRecord(actual, SchemaService.ARDCv1JSONLD);
		assertThat(actualVersion.isCurrent()).isTrue();
		assertThat(actualVersion).isNotNull();
		assertThat(actualVersion.getContent()).isNotEmpty();

		// if process again, there shouldn't be any change
		Record processedAgain = processor.process(record);
		assertThat(processedAgain).isNotNull();
		assertThat(processedAgain.getCurrentVersions()).hasSize(2);
	}

}