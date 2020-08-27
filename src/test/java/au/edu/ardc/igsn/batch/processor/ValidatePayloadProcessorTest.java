package au.edu.ardc.igsn.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import au.edu.ardc.igsn.util.Helpers;

@ExtendWith(SpringExtension.class)
public class ValidatePayloadProcessorTest {
	
	@Test
	public void getContentType_xml()
	{
		ValidatePayloadProcessor p = new ValidatePayloadProcessor();
		try {
		String content_type = p.getContentType(new File("src/test/resources/xml/sample_igsn_csiro_v3.xml"));
		assertThat(content_type.equals(new String("application/xml")));

		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
	}
	
	
	@Test
	public void getContentType_json()
	{
		ValidatePayloadProcessor p = new ValidatePayloadProcessor();
		try {
		String content_type = p.getContentType(new File("src/test/resources/json/json_ld.json"));
		assertThat(content_type.equals(new String("application/json")));
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
	}
	
	@Test void findDocumentNamespcae() throws Exception
	{
		ValidatePayloadProcessor p = new ValidatePayloadProcessor();
		String xml = Helpers.readFile("src/test/resources/xml/sample_igsn_csiro_v3.xml");
		String xmlnamespace = p.getDefaultXMLnameSpace(xml);
		assertThat(xmlnamespace.equals("https://igsn.csiro.au/schemas/3.0"));
	}
	
	@Test void validateDocument_1() throws Exception
	{
		ValidatePayloadProcessor p = new ValidatePayloadProcessor();
		String xml = Helpers.readFile("src/test/resources/xml/sample_igsn_csiro_v3.xml");
		p.service.loadSchemas();
        assertThat(p.service.getSchemas()).isNotNull();
		boolean isValid = p.validate(xml);
		System.out.print("valid:" + isValid);
		assertThat(isValid);
		
	}
	
	@Test void validateDocument_2() throws Exception
	{
		ValidatePayloadProcessor p = new ValidatePayloadProcessor();
		String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		p.service.loadSchemas();
        assertThat(p.service.getSchemas()).isNotNull();
		boolean isValid = p.validate(xml);
		System.out.print("valid:" + isValid);
		assertThat(isValid);
		
	}

}
