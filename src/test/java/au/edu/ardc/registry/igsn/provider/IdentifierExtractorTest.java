package au.edu.ardc.registry.igsn.provider;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.util.Helpers;

public class IdentifierExtractorTest {

	@Autowired
	SchemaService service;
	@Test
	public void extractIdentifierFromARDCV1() throws Exception
	{
		IGSNIdentifierProvider xie = new IGSNIdentifierProvider();
		SchemaService service = new SchemaService();
		service.loadSchemas();
		Schema schema = service.getSchemaByID(SchemaService.ARDCv1);
		String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		String identifierValue = xie.get(schema, xml);
		assertTrue(identifierValue.equals("10273/XX0TUIAYLV"));
		
	}
}
