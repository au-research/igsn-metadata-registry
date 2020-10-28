package au.edu.ardc.registry.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.util.Date;

import org.junit.jupiter.api.Test;

public class HelpersTest {

	@Test
	public void getContentType_xml() {

		try {
			String content_type = Helpers.probeContentType(new File("src/test/resources/xml/sample_igsn_csiro_v3.xml"));
			assertEquals(new String("application/xml"), content_type);
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}

	}

	@Test
	public void getFileExtension_xml() {
		try {
			String content = Helpers.readFile("src/test/resources/xml/sample_igsn_csiro_v3.xml");
			String fileExt = Helpers.getFileExtensionForContent(content);
			assertEquals(new String(".xml"), fileExt);
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}

	}

	@Test
	public void getContentType_json() {
		try {
			String content_type = Helpers.probeContentType(new File("src/test/resources/json/json_ld.json"));
			assertEquals(new String("application/json"), content_type);
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	@Test
	void convertDate() {
		String ISO8601Date = "2011-12-03";
		String ISO8601DateTime = "2020-09-27T12:56:47Z";

		Date newDate = Helpers.convertDate(ISO8601Date);
		assertThat(newDate).isInstanceOf(Date.class);

		Date newDateTime = Helpers.convertDate(ISO8601DateTime);
		assertThat(newDateTime).isInstanceOf(Date.class);
	}

}