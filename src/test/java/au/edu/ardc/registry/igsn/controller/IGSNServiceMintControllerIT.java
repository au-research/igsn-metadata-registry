package au.edu.ardc.registry.igsn.controller;

import au.edu.ardc.registry.KeycloakIntegrationTest;
import au.edu.ardc.registry.TestHelper;
import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.common.model.Scope;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.service.IdentifierService;
import au.edu.ardc.registry.common.service.KeycloakService;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.igsn.model.IGSNAllocation;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class IGSNServiceMintControllerIT extends KeycloakIntegrationTest {

	public static MockWebServer mockMDS;

	private final String baseUrl = "/api/services/igsn/mint/";

	@MockBean
	KeycloakService kcService;

	@Autowired
	IdentifierService identifierService;

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
	@DisplayName("403 because user does not have access to this Identifier")
	void mint_403() throws Exception {
		String validXML = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");

		String mockedServerURL = String.format("http://localhost:%s", mockMDS.getPort());

		User user = TestHelper.mockUser();
		IGSNAllocation allocation = TestHelper.mockIGSNAllocation();
		allocation.setScopes(Arrays.asList(Scope.CREATE, Scope.UPDATE));
		allocation.setPrefix("20.500.11812");
		allocation.setNamespace("XXZT1");
		allocation.setName("Mocked up test Allocation");
		allocation.setMds_username("ANDS.IGSN");
		allocation.setMds_url(mockedServerURL);
		user.setAllocations(Collections.singletonList(allocation));
		when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(user);
		when(kcService.getAllocationByResourceID(anyString())).thenReturn(allocation);

		this.webTestClient.post().uri(baseUrl).header("Authorization", getBasicAuthenticationHeader(username, password))
				.body(Mono.just(validXML), String.class).exchange().expectStatus().isForbidden();
	}

	@Test
	@DisplayName("401 because user is not logged in")
	void mint_401() throws IOException {
		String validXML = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");

		this.webTestClient.post().uri(baseUrl).body(Mono.just(validXML), String.class).exchange().expectStatus()
				.isUnauthorized();
	}

	@Test
	void mint_202() throws Exception {
		String validXML = Helpers.readFile("src/test/resources/xml/sample_mintable_ardcv1.xml");
		String identifierValue = "20.500.11812/XXZT1000023";

		// getting the mocked Server URL so that we can inject it to our mocked up
		// Allocation
		String mockedServerURL = String.format("http://localhost:%s", mockMDS.getPort());
		User user = TestHelper.mockUser();
		IGSNAllocation allocation = TestHelper.mockIGSNAllocation();
		allocation.setScopes(Arrays.asList(Scope.CREATE, Scope.UPDATE));
		allocation.setPrefix("20.500.11812");
		allocation.setNamespace("XXZT1");
		allocation.setName("Mocked up test Allocation");
		allocation.setMds_username("ANDS.IGSN");
		allocation.setMds_url(mockedServerURL);
		user.setAllocations(Collections.singletonList(allocation));

		// the entire authentication model is mocked for this purpose
		when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(user);
		when(kcService.getAllocationByResourceID(anyString())).thenReturn(allocation);

		// Queue 201 returns from MDS twice, 1 for Identifier creation and 1 for Metadata
		// Creation
		mockMDS.enqueue(new MockResponse().setBody("OK").setResponseCode(201));
		mockMDS.enqueue(new MockResponse().setBody("OK").setResponseCode(201));

		// @formatter:off
		// perform the request, 202 should be returned, wait=1 so it runs synchronously
		this.webTestClient.post()
				.uri(uriBuilder -> uriBuilder.path(baseUrl)
						.queryParam("wait", "1").build())
				.header("Authorization", getBasicAuthenticationHeader(username, password))
				.body(Mono.just(validXML), String.class)
				.exchange()
				.expectStatus().isAccepted();
		// @formatter:on

		// the identifier is minted
		Identifier identifier = identifierService.findByValueAndType(identifierValue, Identifier.Type.IGSN);
		assertThat(identifier).isNotNull();
		assertThat(identifier).isInstanceOf(Identifier.class);
		assertThat(identifier.getStatus()).isEqualTo(Identifier.Status.ACCESSIBLE);

		// the record is created and is visible, correct ownership
		Record record = identifier.getRecord();
		assertThat(record).isInstanceOf(Record.class);
		assertThat(record.getOwnerID()).isEqualTo(user.getId());
		assertThat(record.getOwnerType()).isEqualTo(Record.OwnerType.User);
		assertThat(record.isVisible()).isTrue();

		// versions are also created
		List<Version> currentVersions = record.getCurrentVersions();
		assertThat(currentVersions).hasSizeGreaterThan(0);

		// ardcv1 version is created
		Version ardcv1Version = record.getCurrentVersions().stream()
				.filter(version -> version.getSchema().equals(SchemaService.ARDCv1)).findAny().orElse(null);
		assertThat(ardcv1Version).isNotNull();
		assertThat(ardcv1Version.getContent()).isNotEmpty();

		// json-ld is also created
//		Version jsonldVersion = record.getCurrentVersions().stream()
//				.filter(version -> version.getSchema().equals(SchemaService.ARDCv1JSONLD)).findAny().orElse(null);
//		assertThat(jsonldVersion).isNotNull();
//		assertThat(jsonldVersion.getContent()).isNotEmpty();
	}

}