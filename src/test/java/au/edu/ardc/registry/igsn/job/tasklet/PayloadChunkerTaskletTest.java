package au.edu.ardc.registry.igsn.job.tasklet;

import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.util.Helpers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PayloadChunkerTaskletTest {

	@Test
	@DisplayName("Test for chunking batch of 3 ardc IGSN records")
	public void chunksXMLContent() throws Exception {
		String dataPath = "/tmp/" + UUID.randomUUID().toString();
		Helpers.newOrEmptyDirecory(dataPath);
		String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1_batch.xml");
		String payLoadContentPath = dataPath + File.separator + "payload.xml";
		Helpers.writeFile(payLoadContentPath, xml);
		PayloadChunkerTasklet t = new PayloadChunkerTasklet();
		SchemaService service = new SchemaService();
		service.loadSchemas();
		t.setSchemaService(service);
		t.setDirectory(dataPath);
		t.setPayloadPath(payLoadContentPath);
		StepContribution contribution = Mockito.mock(StepContribution.class);
		ChunkContext chunkContext = Mockito.mock(ChunkContext.class);
		t.execute(contribution, chunkContext);
		File outputFile = new File(dataPath + File.separator + "1.xml");
		assertTrue(outputFile.exists());
	}

	@Test
	@DisplayName("Test for chunking batch of 1 ardc IGSN record")
	public void chunksXMLContent_single() throws Exception {
		String dataPath = "/tmp/" + UUID.randomUUID().toString();
		Helpers.newOrEmptyDirecory(dataPath);
		String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		String payLoadContentPath = dataPath + File.separator + "payload.xml";
		Helpers.writeFile(payLoadContentPath, xml);
		PayloadChunkerTasklet t = new PayloadChunkerTasklet();
		SchemaService service = new SchemaService();
		service.loadSchemas();
		t.setSchemaService(service);
		t.setDirectory(dataPath);
		t.setPayloadPath(payLoadContentPath);
		StepContribution contribution = Mockito.mock(StepContribution.class);
		ChunkContext chunkContext = Mockito.mock(ChunkContext.class);
		t.execute(contribution, chunkContext);
		File outputFile = new File(dataPath + File.separator + "0.xml");
		assertTrue(outputFile.exists());
		File shouldntExistOutFile = new File(dataPath + File.separator + "1.xml");
		assertFalse(shouldntExistOutFile.exists());
	}

}