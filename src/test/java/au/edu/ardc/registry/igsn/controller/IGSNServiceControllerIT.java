package au.edu.ardc.registry.igsn.controller;

import au.edu.ardc.registry.KeycloakIntegrationTest;
import au.edu.ardc.registry.TestHelper;
import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.common.model.Scope;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.repository.IdentifierRepository;
import au.edu.ardc.registry.common.repository.RecordRepository;
import au.edu.ardc.registry.common.service.IdentifierService;
import au.edu.ardc.registry.common.service.KeycloakService;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.igsn.model.IGSNAllocation;
import au.edu.ardc.registry.igsn.service.IGSNRecordService;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class IGSNServiceControllerIT extends KeycloakIntegrationTest {

	public static MockWebServer mockMDS;

	private final String mintEndpoint = "/api/services/igsn/mint/";

	private final String updateEndpoint = "/api/services/igsn/update/";

	private final String reserveEndpoint = "/api/services/igsn/reserve/";

	private final String transferEndpoint = "/api/services/igsn/transfer/";

	@MockBean
	KeycloakService kcService;

	@Autowired
	IdentifierService identifierService;

	@Autowired
	IdentifierRepository identifierRepository;

	@Autowired
	RecordRepository recordRepository;

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
		IGSNAllocation allocation = getStubAllocation();
		User user = TestHelper.mockUser();
		user.setAllocations(Collections.singletonList(allocation));

		when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(user);
		when(kcService.getAllocationByResourceID(anyString())).thenReturn(allocation);

		this.webTestClient.post().uri(mintEndpoint)
				.header("Authorization", getBasicAuthenticationHeader(username, password))
				.body(Mono.just(validXML), String.class).exchange().expectStatus().isForbidden();
	}

	@Test
	@DisplayName("401 because user is not logged in")
	void mint_401() throws IOException {
		String validXML = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");

		this.webTestClient.post().uri(mintEndpoint).body(Mono.just(validXML), String.class).exchange().expectStatus()
				.isUnauthorized();
	}

	@Test
	void mint_201() throws Exception {
		String validXML = Helpers.readFile("src/test/resources/xml/sample_mintable_ardcv1.xml");
		String identifierValue = "20.500.11812/XXZT1000023";
		IGSNAllocation allocation = getStubAllocation();
		User user = TestHelper.mockUser();
		user.setAllocations(Collections.singletonList(allocation));

		// the entire authentication model is mocked for this purpose
		when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(user);
		when(kcService.getAllocationByResourceID(anyString())).thenReturn(allocation);

		// Queue 201 returns from MDS twice,
		// - 1 for Identifier creation
		// - 1 for Metadata Creation
		mockMDS.enqueue(new MockResponse().setBody("OK").setResponseCode(201));
		mockMDS.enqueue(new MockResponse().setBody("OK").setResponseCode(201));

		// @formatter:off
		// perform the request, 201 should be returned
		this.webTestClient.post()
				.uri(uriBuilder -> uriBuilder.path(mintEndpoint)
						.build())
				.header("Authorization", getBasicAuthenticationHeader(username, password))
				.body(Mono.just(validXML), String.class)
				.exchange()
				.expectStatus().isCreated();
		// @formatter:on

		// the identifier is created
		Identifier identifier = identifierService.findByValueAndType(identifierValue, Identifier.Type.IGSN);
		assertThat(identifier).isNotNull();
		assertThat(identifier).isInstanceOf(Identifier.class);

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
		// Version jsonldVersion = record.getCurrentVersions().stream()
		// .filter(version ->
		// version.getSchema().equals(SchemaService.ARDCv1JSONLD)).findAny().orElse(null);
		// assertThat(jsonldVersion).isNotNull();
		// assertThat(jsonldVersion.getContent()).isNotEmpty();
	}

	@Test
	@DisplayName("403 because user does not have access to this Identifier")
	void update_403() throws Exception {
		String validXML = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		IGSNAllocation allocation = getStubAllocation();
		User user = TestHelper.mockUser();
		user.setAllocations(Collections.singletonList(allocation));

		when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(user);
		when(kcService.getAllocationByResourceID(anyString())).thenReturn(allocation);

		this.webTestClient.post().uri(updateEndpoint)
				.header("Authorization", getBasicAuthenticationHeader(username, password))
				.body(Mono.just(validXML), String.class).exchange().expectStatus().isForbidden();
	}

	@Test
	@DisplayName("401 in update because the content is invalid")
	void update_invalid_400() throws Exception {
		String invalidXML = Helpers.readFile("src/test/resources/xml/invalid_sample_igsn_csiro_v3.xml");
		IGSNAllocation allocation = getStubAllocation();
		User user = TestHelper.mockUser();
		user.setAllocations(Collections.singletonList(allocation));

		when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(user);
		when(kcService.getAllocationByResourceID(anyString())).thenReturn(allocation);

		this.webTestClient.post().uri(updateEndpoint)
				.header("Authorization", getBasicAuthenticationHeader(username, password))
				.body(Mono.just(invalidXML), String.class).exchange().expectStatus().isBadRequest();
	}

	@Test
	@DisplayName("400 when content not supported")
	void update_not_supported_content_400() throws Exception {
		String invalidXML = Helpers.readFile("src/test/resources/xml/rifcs_sample.xml");
		IGSNAllocation allocation = getStubAllocation();
		User user = TestHelper.mockUser();
		user.setAllocations(Collections.singletonList(allocation));

		when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(user);
		when(kcService.getAllocationByResourceID(anyString())).thenReturn(allocation);

		this.webTestClient.post().uri(updateEndpoint)
				.header("Authorization", getBasicAuthenticationHeader(username, password))
				.body(Mono.just(invalidXML), String.class).exchange().expectStatus().isBadRequest();
	}

	// todo rewrite this test
	void reserve_validRequest_producesReservedIGSN200() throws Exception {
		IGSNAllocation allocation = getStubAllocation();
		User user = TestHelper.mockUser();
		user.setAllocations(Collections.singletonList(allocation));
		when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(user);
		when(kcService.getAllocationByResourceID(anyString())).thenReturn(allocation);

		// @formatter:off
		String requestBody = "20.500.11812/XXAA1234567\n20.500.11812/XXAB12345";
		this.webTestClient.post()
				.uri(uriBuilder -> uriBuilder.path(reserveEndpoint).queryParam("allocationID", allocation.getId()).build())
				.header("Authorization", getBasicAuthenticationHeader(username, password))
				.body(Mono.just(requestBody), String.class)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.id").exists()
				.jsonPath("$.status").exists();
		// @formatter:on

		// 2 identifiers are created
		assertThat(identifierService.findByValueAndType("20.500.11812/XXAA1234567", Identifier.Type.IGSN));
		assertThat(identifierService.findByValueAndType("20.500.11812/XXAB12345", Identifier.Type.IGSN));

		// they are in reserved status
		Identifier identifier = identifierService.findByValueAndType("20.500.11812/XXAA1234567", Identifier.Type.IGSN);
		assertThat(identifier.getStatus()).isEqualTo(Identifier.Status.RESERVED);

		// associating record check (is not visible, has request ID)
		Record record = identifier.getRecord();
		assertThat(record).isNotNull();
		assertThat(record.getType()).isEqualTo(IGSNRecordService.recordType);
		assertThat(record.isVisible()).isFalse();
		assertThat(record.getRequestID()).isNotNull();
		assertThat(record.getCreatedAt()).isNotNull();
		assertThat(record.getCreatorID()).isEqualTo(user.getId());
		assertThat(record.getAllocationID()).isEqualTo(allocation.getId());
	}

	// todo rewrite this test
	void transfer_validRequest_transferedToNewOwner() throws Exception {
		IGSNAllocation allocation = getStubAllocation();
		User user = TestHelper.mockUser();
		user.setAllocations(Collections.singletonList(allocation));
		when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(user);
		when(kcService.getAllocationByResourceID(anyString())).thenReturn(allocation);

		String[] identifierValues = { "12073/XXAA123456", "12073/XXAB123456" };

		for (String identifierValue : identifierValues) {
			Record record = TestHelper.mockRecord();
			recordRepository.saveAndFlush(record);
			Identifier identifier = TestHelper.mockIdentifier();
			identifier.setValue(identifierValue);
			identifier.setRecord(record);
			identifierRepository.saveAndFlush(identifier);
		}

		String targetOwnerType = String.valueOf(Record.OwnerType.DataCenter);
		String targetOwnerID = UUID.randomUUID().toString();

		String requestBody = "12073/XXAAabcDEFG\n12073/XXABabcdEFG";
		this.webTestClient.post()
				.uri(uriBuilder -> uriBuilder.path(transferEndpoint).queryParam("ownerID", targetOwnerID)
						.queryParam("ownerType", targetOwnerType).build())
				.header("Authorization", getBasicAuthenticationHeader(username, password))
				.body(Mono.just(requestBody), String.class).exchange().expectStatus().isOk().expectBody()
				.jsonPath("$.id").exists().jsonPath("$.status").exists();

		Identifier identifier = identifierRepository.findFirstByValueIgnoreCaseAndType("12073/XXAAABCDefg", Identifier.Type.IGSN);
		assertThat(identifier.getRecord().getOwnerID()).isEqualTo(UUID.fromString(targetOwnerID));
		assertThat(identifier.getRecord().getOwnerType()).isEqualTo(Record.OwnerType.valueOf(targetOwnerType));
	}

	/**
	 * Internal helper method to obtain a stub allocation with connection to the mocked
	 * server
	 * @return the {@link IGSNAllocation} with MDS URL point to the mockedServer
	 */
	private IGSNAllocation getStubAllocation() {
		String mockedServerURL = String.format("http://localhost:%s", mockMDS.getPort());
		IGSNAllocation allocation = TestHelper.mockIGSNAllocation();
		allocation.setScopes(Arrays.asList(Scope.CREATE, Scope.UPDATE));
		allocation.setPrefix("20.500.11812");
		allocation.setNamespace("XXZT1");
		allocation.setName("Mocked up test Allocation");
		allocation.setMds_username("ANDS.IGSN");
		allocation.setMds_url(mockedServerURL);
		return allocation;
	}

}