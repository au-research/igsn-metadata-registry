package au.edu.ardc.registry.igsn.client;

import au.edu.ardc.registry.IntegrationTest;
import au.edu.ardc.registry.TestHelper;
import au.edu.ardc.registry.common.model.Scope;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.service.KeycloakService;
import au.edu.ardc.registry.igsn.model.IGSNAllocation;
import clover.org.apache.commons.lang.RandomStringUtils;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class MDSClientIT extends IntegrationTest {

	public static MockWebServer mockMDS;

	@BeforeAll
	static void setUp() throws IOException {
		mockMDS = new MockWebServer();
		mockMDS.start();
	}

	@AfterAll
	static void tearDown() throws IOException {
		mockMDS.shutdown();
	}

	@Test
	public void mintIGSNTest() {

		String mockedServerURL = String.format("http://localhost:%s", mockMDS.getPort());

		IGSNAllocation allocation = TestHelper.mockIGSNAllocation();
		allocation.setScopes(Arrays.asList(Scope.CREATE, Scope.UPDATE));
		allocation.setPrefix("20.500.11812");
		allocation.setNamespace("XXZT1");
		allocation.setName("Mocked up test Allocation");
		allocation.setMds_username("ANDS.IGSN");
		allocation.setMds_url(mockedServerURL);

		MDSClient mc = new MDSClient(allocation);
		int response_code = 0;
		SimpleDateFormat df = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ssXXX");
		String sampleNumber = RandomStringUtils.randomAlphabetic(10).toUpperCase();
		String identifier = allocation.getPrefix() + "/" + allocation.getNamespace() + sampleNumber;
		String metacontent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
		metacontent += "<sample xmlns=\"http://igsn.org/schema/kernel-v.1.0\" "
				+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
				+ "xsi:schemaLocation=\"http://igsn.org/schema/kernel-v.1.0 "
				+ "http://doidb.wdc-terra.org/igsn/schemas/igsn.org/schema/1.0/igsn.xsd\">";
		metacontent += "<sampleNumber identifierType=\"igsn\">" + identifier + "</sampleNumber>";
		metacontent += "<registrant><registrantName>" + allocation.getMds_username() + "</registrantName></registrant>";
		metacontent += "<log><logElement event=\"registered\" timeStamp=\"" + df.format(new Date())
				+ "\"/></log></sample>";
		String landingPage = "http://somewhere.com/landing/" + identifier;

		mockMDS.enqueue(new MockResponse().setBody("OK").setResponseCode(201));
		mockMDS.enqueue(new MockResponse().setBody("OK").setResponseCode(201));
		try {
			response_code = mc.createOrUpdateIdentifier(identifier, landingPage);
			response_code = mc.addMetadata(metacontent);
		}
		catch (Exception e) {
			System.out.print(e.getMessage());
		}
		assertEquals(201, response_code);
	}

	@Test
	public void getIGSNLandingPageTest() {
		String mockedServerURL = String.format("http://localhost:%s", mockMDS.getPort());
		IGSNAllocation ia = TestHelper.mockIGSNAllocation();
		ia.setMds_username("username").setMds_password("password").setMds_url(mockedServerURL);
		MDSClient mc = new MDSClient(ia);
		String identifier = "20.500.11812/XXAAAURRXDFVJA";
		String landingPage = "";
		mockMDS.enqueue(
				new MockResponse().setBody("https://test.handle.ardc.edu.au/igsn/#/meta/20.500.11812/XXAAAURRXDFVJA")
						.setResponseCode(200));
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
		String mockedServerURL = String.format("http://localhost:%s", mockMDS.getPort());
		IGSNAllocation ia = TestHelper.mockIGSNAllocation();
		ia.setMds_username("username").setMds_password("password").setMds_url(mockedServerURL);
		MDSClient mc = new MDSClient(ia);
		String identifier = "20.500.11812/XXAAAURRXDFVJA";
		String metadata = "";
		mockMDS.enqueue(new MockResponse().setBody("20.500.11812/XXAAAURRXDFVJA").setResponseCode(200));
		try {
			metadata = mc.getIGSNMetadata(identifier);
		}
		catch (Exception e) {
			System.out.print(e.getMessage());
		}
		assertTrue(metadata.contains(identifier));
	}

}
