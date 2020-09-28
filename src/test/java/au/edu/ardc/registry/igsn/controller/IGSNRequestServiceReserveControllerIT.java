package au.edu.ardc.registry.igsn.controller;

import au.edu.ardc.registry.KeycloakIntegrationTest;
import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.repository.IdentifierRepository;
import au.edu.ardc.registry.igsn.service.IGSNRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IGSNRequestServiceReserveControllerIT extends KeycloakIntegrationTest {

	private final String baseUrl = "/api/services/igsn/reserve";

	@Autowired
	IdentifierRepository identifierRepository;

	@Test
	void reserve_NotLoggedIn_401() {
		this.webTestClient.post().uri(baseUrl).exchange().expectStatus().isUnauthorized();
	}

	@Test
	void reserve_validRequest_producesReservedIGSN200() {
		// @formatter:off
		String requestBody = "12073/XXAA1234567\n12703/XXAB12345";
		this.webTestClient.post()
				.uri(uriBuilder -> uriBuilder.path(baseUrl).queryParam("allocationID", resourceID).build())
				.header("Authorization", getBasicAuthenticationHeader(username, password))
				.body(Mono.just(requestBody), String.class)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.id").exists()
				.jsonPath("$.status").exists();
		// @formatter:on

		// 2 identifiers are created
		assertThat(identifierRepository.existsByTypeAndValue(Identifier.Type.IGSN, "12073/XXAA1234567"));
		assertThat(identifierRepository.existsByTypeAndValue(Identifier.Type.IGSN, "12703/XXAB12345"));

		// they are in reserved status
		Identifier identifier = identifierRepository.findFirstByValueAndType("12073/XXAA1234567", Identifier.Type.IGSN);
		assertThat(identifier.getStatus()).isEqualTo(Identifier.Status.RESERVED);

		// associating record check (is not visible, has request ID)
		Record record = identifier.getRecord();
		assertThat(record).isNotNull();
		assertThat(record.getType()).isEqualTo(IGSNRecordService.recordType);
		assertThat(record.isVisible()).isFalse();
		assertThat(record.getRequestID()).isNotNull();
		assertThat(record.getCreatedAt()).isNotNull();
		assertThat(record.getCreatorID()).isEqualTo(UUID.fromString(userID));
		assertThat(record.getAllocationID()).isEqualTo(UUID.fromString(resourceID));
	}

}