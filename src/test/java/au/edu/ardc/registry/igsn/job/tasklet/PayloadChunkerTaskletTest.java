package au.edu.ardc.registry.igsn.job.tasklet;

import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.util.Helpers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.batch.test.StepScopeTestExecutionListener;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import java.io.File;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBatchTest
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, StepScopeTestExecutionListener.class })
class PayloadChunkerTaskletTest {

	@Autowired
	SchemaService service;

	@Autowired
	protected JobLauncher jobLauncher;

	@Autowired
	JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	PayloadChunkerTasklet payloadChunkerTasklet;

	@Autowired
	@Qualifier("IGSNImportJob")
	protected Job job;

	// TODO find out best way to test individual steps
	/*
	 * @Test
	 *
	 * @DisplayName("Test for chunking batch of 3 ardc IGSN records") public void
	 * chunksXMLContent() throws Exception { String dataPath = "/tmp/" +
	 * UUID.randomUUID().toString(); Helpers.newOrEmptyDirecory(dataPath); String xml =
	 * Helpers.readFile("src/test/resources/xml/sample_ardcv1_batch.xml"); String
	 * payLoadContentPath = dataPath + File.separator + "payload.xml";
	 * Helpers.writeFile(payLoadContentPath, xml); PayloadChunkerTasklet t = new
	 * PayloadChunkerTasklet(); StepExecution execution = createStepExecution(dataPath,
	 * payLoadContentPath); jobLauncherTestUtils.launchStep("chunk",
	 * execution.getExecutionContext()); File outputFile = new File(dataPath +
	 * File.separator + "1.xml"); assertTrue(outputFile.exists()); }
	 */

	private StepExecution createStepExecution(String dataPath, String payLoadContentPath) {
		StepExecution execution = MetaDataInstanceFactory.createStepExecution();
		execution.getExecutionContext().putString("payLoadContentFile", payLoadContentPath);
		execution.getExecutionContext().putString("chunkContentsDir", dataPath + File.separator + "chunks");
		execution.getExecutionContext().putString("dataPath", dataPath);
		return execution;
	}

}