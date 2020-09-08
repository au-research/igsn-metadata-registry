package au.edu.ardc.registry.igsn.client;

import au.edu.ardc.registry.IntegrationTest;
import au.edu.ardc.registry.TestHelper;
import au.edu.ardc.registry.igsn.model.IGSNAllocation;
import clover.org.apache.commons.lang.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@EnabledIf(expression = "${mds.enabled}", reason = "Disable test if MDS is not configured", loadContext = true)
public class MDSClientIT extends IntegrationTest {

	@Value("${mds.username}")
	private String mds_user_name;

	@Value("${mds.password}")
	private String mds_user_password;

	@Value("${mds.url}")
	private String mds_url;

	@Value("${landing_page_base_url}")
	private String base_url;

	@Test
	public void getUrlTest() {
		IGSNAllocation ia = TestHelper.mockIGSNAllocation();
		ia.setMds_username(this.mds_user_name).setMds_password(this.mds_user_password).setMds_url(this.mds_url);
		MDSClient mc = new MDSClient(ia);
		String url = mc.getUrl();
		System.out.println("URL IS " + url);
		assertEquals("https://doidb.wdc-terra.org/igsn/", url);
	}

	@Test
	public void mintIGSNTest() {
		IGSNAllocation ia = TestHelper.mockIGSNAllocation();
		ia.setMds_username(this.mds_user_name).setMds_password(this.mds_user_password).setMds_url(this.mds_url);
		MDSClient mc = new MDSClient(ia);
		int response_code = 0;
		SimpleDateFormat df = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ssXXX");
		String sampleNumber = RandomStringUtils.randomAlphabetic(10).toUpperCase();
		String identifier = ia.getPrefix() + "/" + ia.getNamespace() + sampleNumber;
		String metacontent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
		metacontent += "<sample xmlns=\"http://igsn.org/schema/kernel-v.1.0\" "
				+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
				+ "xsi:schemaLocation=\"http://igsn.org/schema/kernel-v.1.0 "
				+ "http://doidb.wdc-terra.org/igsn/schemas/igsn.org/schema/1.0/igsn.xsd\">";
		metacontent += "<sampleNumber identifierType=\"igsn\">" + identifier + "</sampleNumber>";
		metacontent += "<registrant><registrantName>" + this.mds_user_name + "</registrantName></registrant>";
		metacontent += "<log><logElement event=\"registered\" timeStamp=\"" + df.format(new Date())
				+ "\"/></log></sample>";
		String landingPage = this.base_url + identifier;
		try {
			response_code = mc.mintIGSN(metacontent, identifier, landingPage, false);
		}
		catch (Exception e) {
			System.out.print(e.getMessage());
		}
		assertEquals(201, response_code);
	}

	@Test
	public void getIGSNLandingPageTest() {
		IGSNAllocation ia = TestHelper.mockIGSNAllocation();
		ia.setMds_username(this.mds_user_name).setMds_password(this.mds_user_password).setMds_url(this.mds_url);
		MDSClient mc = new MDSClient(ia);
		String identifier = "20.500.11812/XXAAAURRXDFVJA";
		String landingPage = "";
		try {
			landingPage = mc.getIGSNLandingPage(identifier);
		}
		catch (Exception e) {
			System.out.print(e.getMessage());
		}
		assertEquals("https://test.handle.ardc.edu.au/igsn/#/meta/20.500.11812/XXAAAURRXDFVJA", landingPage);

	}

	@Test
	public void getIGSNMetadataTest() {
		IGSNAllocation ia = TestHelper.mockIGSNAllocation();
		ia.setMds_username(this.mds_user_name).setMds_password(this.mds_user_password).setMds_url(this.mds_url);
		MDSClient mc = new MDSClient(ia);
		String identifier = "20.500.11812/XXAAAURRXDFVJA";
		String metadata = "";
		try {
			metadata = mc.getIGSNMetadata(identifier);
		}
		catch (Exception e) {
			System.out.print(e.getMessage());
		}
		assertTrue(metadata.contains(identifier));
	}

}
