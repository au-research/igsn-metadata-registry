package au.edu.ardc.registry.igsn.controller;

import au.edu.ardc.registry.KeycloakIntegrationTest;
import au.edu.ardc.registry.TestHelper;
import au.edu.ardc.registry.common.model.Scope;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.service.KeycloakService;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.igsn.model.IGSNAllocation;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class MintIGSNResourceControllerIT extends KeycloakIntegrationTest {

	public static MockWebServer mockMDS;
	private final String baseUrl = "/api/services/igsn/mint/";
	@MockBean
	KeycloakService kcService;

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

		mockMDS.enqueue(new MockResponse().setBody("OK").setResponseCode(201));
		mockMDS.enqueue(new MockResponse().setBody("OK").setResponseCode(201));

		// @formatter:off
		this.webTestClient.post()
				.uri(baseUrl)
				.header("Authorization", getBasicAuthenticationHeader(username, password))
				.body(Mono.just(validXML), String.class)
				.exchange()
				.expectStatus().isAccepted();
		// @formatter:on
	}

}