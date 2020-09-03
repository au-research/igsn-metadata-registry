package au.edu.ardc.registry.job.processor;

import au.edu.ardc.registry.TestHelper;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.common.service.RecordService;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.service.VersionService;
import au.edu.ardc.registry.common.util.Helpers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { SchemaService.class })
class RecordTitleProcessorTest {

	@MockBean
	VersionService versionService;

	@MockBean
	RecordService recordService;

	@Autowired
	SchemaService schemaService;

	@Test
	void process_updatesTitles() throws Exception {
		// record without title and a version with the title
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		record.setTitle(null);
		Version version = TestHelper.mockVersion(record);
		String validXML = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		version.setContent(validXML.getBytes());
		version.setSchema(SchemaService.ARDCv1);

		// record with title that will be returned
		Record expected = TestHelper.mockRecord(record.getId());
		expected.setTitle("Something");

		// setup the world
		when(versionService.findVersionForRecord(any(Record.class), anyString())).thenReturn(version);
		when(recordService.save(any(Record.class))).thenReturn(expected);

		// when process title
		RecordTitleProcessor processor = new RecordTitleProcessor(versionService, recordService, schemaService);
		Record actual = processor.process(record);

		// actual has title
		assertThat(actual).isNotNull();
		assertThat(actual.getTitle()).isNotNull();

		// save is called to persist the record after title processing
		verify(recordService, times(1)).save(any(Record.class));
	}

}