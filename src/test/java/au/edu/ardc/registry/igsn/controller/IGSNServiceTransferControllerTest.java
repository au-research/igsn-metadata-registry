package au.edu.ardc.registry.igsn.controller;

import au.edu.ardc.registry.KeycloakIntegrationTest;
import au.edu.ardc.registry.TestHelper;
import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.repository.IdentifierRepository;
import au.edu.ardc.registry.common.repository.RecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IGSNServiceTransferControllerTest extends KeycloakIntegrationTest {

	private final String baseUrl = "/api/services/igsn/transfer";

	@Autowired
	RecordRepository recordRepository;

	@Autowired
	IdentifierRepository identifierRepository;

	@Test
	void transfer_NotLoggedIn_401() {
		this.webTestClient.post().uri(baseUrl).exchange().expectStatus().isUnauthorized();
	}

	@Test
	void transfer_validRequest_transferedToNewOwner() {
		String[] identifierValues = { "12073/XXAA123456", "12073/XXAB123456" };

		for (int i = 0; i < 2; i++) {
			Record record = TestHelper.mockRecord();
			recordRepository.saveAndFlush(record);
			Identifier identifier = TestHelper.mockIdentifier();
			identifier.setValue(identifierValues[i]);
			identifier.setRecord(record);
			identifierRepository.saveAndFlush(identifier);
		}

		String targetOwnerType = String.valueOf(Record.OwnerType.DataCenter);
		String targetOwnerID = UUID.randomUUID().toString();

		String requestBody = "12073/XXAA123456\n12073/XXAB123456";
		this.webTestClient.post()
				.uri(uriBuilder -> uriBuilder.path(baseUrl).queryParam("ownerID", targetOwnerID)
						.queryParam("ownerType", targetOwnerType).build())
				.header("Authorization", getBasicAuthenticationHeader(username, password))
				.body(Mono.just(requestBody), String.class).exchange().expectStatus().isOk().expectBody()
				.jsonPath("$.id").exists().jsonPath("$.status").exists();

		Identifier identifier = identifierRepository.findByValueAndType("12073/XXAA123456", Identifier.Type.IGSN);
		assertThat(identifier.getRecord().getOwnerID()).isEqualTo(UUID.fromString(targetOwnerID));
		assertThat(identifier.getRecord().getOwnerType()).isEqualTo(Record.OwnerType.valueOf(targetOwnerType));
	}

}