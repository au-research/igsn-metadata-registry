package au.edu.ardc.igsn.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

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

}
