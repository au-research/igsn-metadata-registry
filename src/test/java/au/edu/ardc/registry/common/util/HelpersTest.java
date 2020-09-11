package au.edu.ardc.registry.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
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

}