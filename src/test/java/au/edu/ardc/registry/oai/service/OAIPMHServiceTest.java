package au.edu.ardc.registry.oai.service;

import au.edu.ardc.registry.common.config.ApplicationProperties;
import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.service.RecordService;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.service.VersionService;
import au.edu.ardc.registry.oai.exception.BadArgumentException;
import au.edu.ardc.registry.oai.exception.CannotDisseminateFormatException;
import au.edu.ardc.registry.oai.response.OAIIdentifyResponse;
import au.edu.ardc.registry.oai.response.OAIResponse;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { OAIPMHService.class, ApplicationProperties.class, SchemaService.class })
class OAIPMHServiceTest {

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
	void isOAIProvider() {
		Schema schema1 = schemaService.getSchemaByID(SchemaService.ARDCv1);
		if (service.isOAIProvider(schema1)) {
			String thenamespace = schema1.getNamespace();
			assertThat(thenamespace).isEqualTo("https://identifiers.ardc.edu.au/schemas/ardc-igsn-desc");
		}
	}

	@Test
	void isOAIProvider_false() {
		Schema schema1 = schemaService.getSchemaByID(SchemaService.CSIROv3);
		if (!service.isOAIProvider(schema1)) {
			String theSchemaId = schema1.getId();
			assertThat(theSchemaId).isEqualTo(SchemaService.CSIROv3);
		}
	}

	@Test
	void getOAIProviders() {
		List<Schema> oaiSchemas = service.getOAIProviders();
		for (Schema schema : oaiSchemas) {
			assertThat(service.isOAIProvider(schema));
		}
	}

	@Test
	void isValidVerb() {
		String verb = "ListRecords";
		assertThat(service.isValidVerb(verb)).isTrue();
	}

	@Test
	void isValidVerb_false() {
		assertThat(service.isValidVerb("nonsenseVerb")).isFalse();
	}

	@Test
	void identify() {
		OAIIdentifyResponse response = (OAIIdentifyResponse) service.identify();
		assertThat(response).isInstanceOf(OAIResponse.class);
		assertThat(response.getIdentify().getRepositoryName()).isEqualTo(applicationProperties.getName());
	}

	@Test
	void getRecord() {
		Assert.assertThrows(BadArgumentException.class, () -> {
			service.getRecord(null, UUID.randomUUID().toString());
		});

		Assert.assertThrows(CannotDisseminateFormatException.class, () -> {
			service.getRecord("nonsense", UUID.randomUUID().toString());
		});

		Assert.assertThrows(BadArgumentException.class, () -> {
			service.getRecord(SchemaService.ARDCv1, null);
		});
	}

	@Test
	void listRecords() {
		Assert.assertThrows(BadArgumentException.class, () -> {
			service.listRecords(null,null, null, null);
		});

		Assert.assertThrows(CannotDisseminateFormatException.class, () -> {
			service.listRecords("nonsense", null, null, null);
		});

		Assert.assertThrows(BadArgumentException.class, () -> {
			service.listRecords(null, null, null, null);
		});
	}


	@Test
	void convertDate(){
		String ISO8601Date = "2011-12-03";
		String ISO8601DateTime = "2020-09-27T12:56:47Z";

		Date newDate = service.convertDate(ISO8601Date);
		assertThat(newDate).isInstanceOf(Date.class);

		Date newDateTime = service.convertDate(ISO8601DateTime);
		assertThat(newDateTime).isInstanceOf(Date.class);
	}
}