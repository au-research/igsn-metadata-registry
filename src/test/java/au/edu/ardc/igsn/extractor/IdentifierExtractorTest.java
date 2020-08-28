package au.edu.ardc.igsn.extractor;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import au.edu.ardc.igsn.entity.Identifier;
import au.edu.ardc.igsn.model.Schema;
import au.edu.ardc.igsn.service.SchemaService;
import au.edu.ardc.igsn.util.Helpers;

public class IdentifierExtractorTest {

	@Autowired
	SchemaService service;
	@Test
	public void extractIdentifierFromARDCV1() throws Exception
	{
		XMLIdentifierExtractor xie = new XMLIdentifierExtractor();
		SchemaService service = new SchemaService();
		service.loadSchemas();
		Schema schema = service.getSchemaByID(SchemaService.ARDCv1);
		String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		String identifierValue = xie.getIdentifier(schema, xml);
		assertTrue(identifierValue.equals("10273/XX0TUIAYLV"));
		
	}
}
