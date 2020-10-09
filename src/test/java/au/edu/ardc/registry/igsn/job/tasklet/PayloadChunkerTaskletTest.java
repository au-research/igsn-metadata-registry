package au.edu.ardc.registry.igsn.job.tasklet;

import au.edu.ardc.registry.common.model.Attribute;
import au.edu.ardc.registry.common.service.*;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.igsn.job.config.IGSNMintJobConfig;
import au.edu.ardc.registry.igsn.service.IGSNRequestService;
import au.edu.ardc.registry.igsn.service.IGSNVersionService;
import au.edu.ardc.registry.job.BatchConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBatchTest
@ExtendWith(SpringExtension.class)
@EnableAutoConfiguration
@ContextConfiguration(classes = { BatchConfig.class, IGSNMintJobConfig.class, SchemaService.class })
class PayloadChunkerTaskletTest {

	@Autowired
	SchemaService schemaService;

	@MockBean
	KeycloakService kcService;

	@MockBean
	IdentifierService identifierService;

	@MockBean
	RecordService recordService;

	@MockBean
	IGSNVersionService igsnVersionService;

	@MockBean
	IGSNRequestService igsnRequestService;

	@MockBean
	URLService urlService;

	@Autowired
	@Qualifier("IGSNImportJob")
	Job job;

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Test
	@DisplayName("When chunk a file, a chunks directory is created with each file being chunked inside")
	void chunkJob_success() throws IOException {

		// given a datapath already containing some files ready to be batched
		Request request = new Request();
		request.setId(UUID.randomUUID());
		String dataPath = "/tmp/" + request.getId().toString();
		Helpers.newOrEmptyDirecory(dataPath);
		String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1_batch.xml");
		String payLoadContentPath = dataPath + File.separator + "payload.xml";
		Helpers.writeFile(payLoadContentPath, xml);

		request.setAttribute(Attribute.DATA_PATH, dataPath)
				.setAttribute(Attribute.PAYLOAD_PATH, payLoadContentPath)
				.setAttribute(Attribute.CHUNKED_PAYLOAD_PATH, dataPath + File.separator + "chunks")
				.setAttribute(Attribute.REQUESTED_IDENTIFIERS_PATH, dataPath + File.separator + "igsn_list.txt");

		when(igsnRequestService.findById(request.getId().toString())).thenReturn(request);

		// @formatter:off
		JobParameters jobParameters = new JobParametersBuilder()
				.addString("IGSNServiceRequestID", request.getId().toString())
				.toJobParameters();
		// @formatter:on

		// when the job is executed
		JobExecution jobExecution = jobLauncherTestUtils.launchStep("chunkPayload", jobParameters);

		// the job finishes successfully
		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		// there's a chunks directory available, has 3 files
		File chunkDirectory = new File(dataPath + "/chunks");
		assertThat(chunkDirectory).exists();
		assertThat(chunkDirectory).isDirectory();
		File[] chunkedFiles = chunkDirectory.listFiles();
		assertThat(chunkedFiles).isNotNull();
		assertThat(chunkedFiles.length).isEqualTo(3);

		// clean up, delete the dataPath
		FileSystemUtils.deleteRecursively(new File(dataPath));
	}

	@Configuration
	@EnableBatchProcessing
	static class BatchTestConfig {

		@Bean
		JobLauncherTestUtils jobLauncherTestUtils() {
			return new JobLauncherTestUtils();
		}

	}

}