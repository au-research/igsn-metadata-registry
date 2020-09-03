package au.edu.ardc.registry.common.util;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import org.junit.jupiter.api.Test;

public class HelpersTest {

	@Test
	public void getContentType_xml() {

		try {
			String content_type = Helpers.probeContentType(new File("src/test/resources/xml/sample_igsn_csiro_v3.xml"));
			assertTrue(content_type.equals(new String("application/xml")));
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}

	}

	@Test
	public void getContentType_json() {
		try {
			String content_type = Helpers.probeContentType(new File("src/test/resources/json/json_ld.json"));
			assertTrue(content_type.equals(new String("application/json")));
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

}