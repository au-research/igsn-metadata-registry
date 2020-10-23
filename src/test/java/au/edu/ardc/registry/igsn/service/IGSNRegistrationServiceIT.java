package au.edu.ardc.registry.igsn.service;

import au.edu.ardc.registry.TestHelper;
import au.edu.ardc.registry.common.entity.*;
import au.edu.ardc.registry.common.model.Attribute;
import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.model.Scope;
import au.edu.ardc.registry.common.service.*;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.exception.ForbiddenOperationException;
import au.edu.ardc.registry.exception.NotFoundException;
import au.edu.ardc.registry.exception.VersionContentAlreadyExistsException;
import au.edu.ardc.registry.igsn.client.MDSClient;
import au.edu.ardc.registry.igsn.model.IGSNAllocation;
import au.edu.ardc.registry.igsn.provider.ardcv1.ARDCv1LandingPageProvider;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.logging.log4j.Level;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ContextConfiguration(classes = { IGSNRegistrationService.class, SchemaService.class })
class IGSNRegistrationServiceIT {

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

	@Autowired
	IGSNRegistrationService igsnRegistrationService;

	@MockBean
	KeycloakService keycloakService;

	@MockBean
	IGSNRequestService igsnRequestService;

	@MockBean
	IdentifierService identifierService;

	@MockBean
	IGSNVersionService igsnVersionService;

	@MockBean
	URLService urlService;

	@Test
	public void throws_exception_if_identifier_doesnt_exist() throws Exception {
		when(igsnRequestService.getLoggerFor(any(Request.class)))
				.thenReturn(TestHelper.getConsoleLogger(ImportServiceTest.class.getName(), Level.DEBUG));
		Request request = TestHelper.mockRequest();
		request.setAttribute(Attribute.OWNER_TYPE, "User");
		request.setAttribute(Attribute.CREATOR_ID, UUID.randomUUID().toString());
		request.setAttribute(Attribute.ALLOCATION_ID, UUID.randomUUID().toString());
		String identifierValue = "10273/XX0TUIAYLV";
		Assert.assertThrows(ForbiddenOperationException.class, () -> {
			igsnRegistrationService.registerIdentifier(identifierValue, request);
		});

	}

	@Test
	public void throws_exception_if_version_doesnt_exist() throws Exception {
		when(igsnRequestService.getLoggerFor(any(Request.class)))
				.thenReturn(TestHelper.getConsoleLogger(ImportServiceTest.class.getName(), Level.DEBUG));
		Request request = TestHelper.mockRequest();
		request.setAttribute(Attribute.OWNER_TYPE, "User");
		request.setAttribute(Attribute.CREATOR_ID, UUID.randomUUID().toString());
		request.setAttribute(Attribute.ALLOCATION_ID, UUID.randomUUID().toString());
		String identifierValue = "10273/XX0TUIAYLV";
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		Identifier identifier = TestHelper.mockIdentifier(record);
		identifier.setType(Identifier.Type.IGSN);
		identifier.setValue(identifierValue);
		when(identifierService.findByValueAndType(identifier.getValue(), Identifier.Type.IGSN)).thenReturn(identifier);
		Assert.assertThrows(NotFoundException.class, () -> {
			igsnRegistrationService.registerIdentifier(identifierValue, request);
		});

	}

	@Test
	public void updates_url_and_registration_metadata() throws Exception {
		String mockedServerURL = String.format("http://localhost:%s", mockMDS.getPort());
		IGSNAllocation allocation = TestHelper.mockIGSNAllocation();
		allocation.setScopes(Arrays.asList(Scope.CREATE, Scope.UPDATE));
		allocation.setPrefix("20.500.11812");
		allocation.setNamespace("XXZT1");
		allocation.setName("Mocked up test Allocation");
		allocation.setMds_username("ANDS.IGSN");
		allocation.setMds_url(mockedServerURL);

		when(igsnRequestService.getLoggerFor(any(Request.class)))
				.thenReturn(TestHelper.getConsoleLogger(ImportServiceTest.class.getName(), Level.DEBUG));
		Request request = TestHelper.mockRequest();
		request.setAttribute(Attribute.OWNER_TYPE, "User");
		request.setAttribute(Attribute.CREATOR_ID, UUID.randomUUID().toString());
		request.setAttribute(Attribute.ALLOCATION_ID, UUID.randomUUID().toString());
		String identifierValue = "10273/XX0TUIAYLV";
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		Identifier identifier = TestHelper.mockIdentifier(record);
		identifier.setType(Identifier.Type.IGSN);
		identifier.setValue(identifierValue);
		when(identifierService.findByValueAndType(identifier.getValue(), Identifier.Type.IGSN)).thenReturn(identifier);

		Version version = TestHelper.mockVersion(record);
		version.setSchema(SchemaService.ARDCv1);
		String content = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		version.setContent(content.getBytes());
		version.setHash(VersionService.getHash(content));
		version.setCreatedAt(request.getCreatedAt());
		record.setCurrentVersions(Collections.singletonList(version));

		when(igsnVersionService.getCurrentVersionForRecord(record, SchemaService.ARDCv1)).thenReturn(version);
		when(keycloakService.getAllocationByResourceID(any())).thenReturn(allocation);

		mockMDS.enqueue(new MockResponse().setBody("OK").setResponseCode(201));
		mockMDS.enqueue(new MockResponse().setBody("OK").setResponseCode(201));
		igsnRegistrationService.registerIdentifier(identifierValue, request);

	}

	@Test
	public void updates_registration_metadata_only() throws Exception {
		String mockedServerURL = String.format("http://localhost:%s", mockMDS.getPort());
		IGSNAllocation allocation = TestHelper.mockIGSNAllocation();
		allocation.setScopes(Arrays.asList(Scope.CREATE, Scope.UPDATE));
		allocation.setPrefix("20.500.11812");
		allocation.setNamespace("XXZT1");
		allocation.setName("Mocked up test Allocation");
		allocation.setMds_username("ANDS.IGSN");
		allocation.setMds_url(mockedServerURL);

		when(igsnRequestService.getLoggerFor(any(Request.class)))
				.thenReturn(TestHelper.getConsoleLogger(ImportServiceTest.class.getName(), Level.DEBUG));
		Request request = TestHelper.mockRequest();
		request.setAttribute(Attribute.OWNER_TYPE, "User");
		request.setAttribute(Attribute.CREATOR_ID, UUID.randomUUID().toString());
		request.setAttribute(Attribute.ALLOCATION_ID, UUID.randomUUID().toString());
		String identifierValue = "10273/XX0TUIAYLV";
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		Identifier identifier = TestHelper.mockIdentifier(record);
		identifier.setType(Identifier.Type.IGSN);
		identifier.setValue(identifierValue);
		when(identifierService.findByValueAndType(identifier.getValue(), Identifier.Type.IGSN)).thenReturn(identifier);

		Version version = TestHelper.mockVersion(record);
		version.setSchema(SchemaService.ARDCv1);
		String content = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		version.setContent(content.getBytes());
		version.setHash(VersionService.getHash(content));
		version.setCreatedAt(request.getCreatedAt());

		URL url = new URL();
		url.setRecord(record);
		url.setUrl("https://demo.identifiers.ardc.edu.au/igsn/#/meta/XX0TUIAYLV");
		when(igsnVersionService.getCurrentVersionForRecord(record, SchemaService.ARDCv1)).thenReturn(version);
		when(urlService.findByRecord(any(Record.class))).thenReturn(url);
		when(keycloakService.getAllocationByResourceID(any())).thenReturn(allocation);

		mockMDS.enqueue(new MockResponse().setBody("OK").setResponseCode(201));
		igsnRegistrationService.registerIdentifier(identifierValue, request);

	}

}