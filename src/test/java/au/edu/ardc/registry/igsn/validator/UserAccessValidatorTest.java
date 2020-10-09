package au.edu.ardc.registry.igsn.validator;

import au.edu.ardc.registry.TestHelper;
import au.edu.ardc.registry.common.dto.mapper.IdentifierMapper;
import au.edu.ardc.registry.common.dto.mapper.RecordMapper;
import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.model.Scope;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.repository.IdentifierRepository;
import au.edu.ardc.registry.common.repository.RecordRepository;
import au.edu.ardc.registry.common.service.IdentifierService;
import au.edu.ardc.registry.common.service.RecordService;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.service.ValidationService;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.exception.ForbiddenOperationException;
import au.edu.ardc.registry.igsn.model.IGSNAllocation;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
		classes = { IdentifierService.class, ValidationService.class, SchemaService.class, RecordService.class })
class UserAccessValidatorTest {

	@Autowired
	IdentifierService identifierService;

	@Autowired
	ValidationService validationService;

	@Autowired
	SchemaService schemaService;

	@MockBean
	IdentifierMapper identifierMapper;

	@MockBean
	RecordMapper recordMapper;

	@MockBean
	IdentifierRepository identifierRepository;

	@MockBean
	RecordRepository recordRepository;

	@Test
	@DisplayName("Test that we can build a UserAccessValidator")
	void constructor() {
		UserAccessValidator userAccessValidator = new UserAccessValidator(identifierService, validationService,
				schemaService);
		Assertions.assertThat(userAccessValidator).isInstanceOf(UserAccessValidator.class);
	}

	@Test
	@DisplayName("Create happy path. User has access to the same prefix/namespace as the one requested in the identifier")
	void canUserCreateIGSNRecord_happyPath() throws IOException {
		UserAccessValidator userAccessValidator = new UserAccessValidator(identifierService, validationService,
				schemaService);
		String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		User user = TestHelper.mockUser();
		IGSNAllocation allocation = TestHelper.mockIGSNAllocation();
		allocation.setPrefix("10273");
		allocation.setNamespace("XX0T");
		allocation.setScopes(Arrays.asList(Scope.CREATE, Scope.UPDATE));
		user.setAllocations(Collections.singletonList(allocation));

		Assert.assertTrue(userAccessValidator.canUserCreateIGSNRecord(xml, user));
	}

	@Test
	@DisplayName("Update happy path. User has access to the same prefix/namespace as the one requested in the identifier")
	void canUserUpdateIGSNRecord_happyPath() throws IOException {
		UserAccessValidator userAccessValidator = new UserAccessValidator(identifierService, validationService,
				schemaService);

		// xml with 10273/XX0T, allocation with the same set, user associated with that
		// allocation
		String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		User user = TestHelper.mockUser();
		IGSNAllocation allocation = TestHelper.mockIGSNAllocation();
		allocation.setPrefix("10273");
		allocation.setNamespace("XX0T");
		allocation.setScopes(Arrays.asList(Scope.CREATE, Scope.UPDATE));
		user.setAllocations(Collections.singletonList(allocation));

		// record exists with the same allocation ID, and the user is the owner
		Record mockRecord = TestHelper.mockRecord(UUID.randomUUID());
		mockRecord.setAllocationID(allocation.getId());
		mockRecord.setOwnerType(Record.OwnerType.User);
		mockRecord.setOwnerID(user.getId());

		// identifier exists in identifierRepository, and linked to the mockedRecord
		Identifier mockedIdentifier = TestHelper.mockIdentifier();
		mockedIdentifier.setValue("10273/XX0TUIAYLV");
		mockedIdentifier.setType(Identifier.Type.IGSN);
		mockedIdentifier.setRecord(mockRecord);
		Mockito.when(identifierRepository.findFirstByValueAndType("10273/XX0TUIAYLV", Identifier.Type.IGSN))
				.thenReturn(mockedIdentifier);

		Assert.assertTrue(userAccessValidator.canUserUpdateIGSNRecord(xml, user));
	}

	@Test
	@DisplayName("User who don't have access to the first identifier will fail with ForbiddenOperationException")
	void canUserCreateIGSNRecord_noAccessToIdentifier_ForbiddenOperationException() throws IOException {
		UserAccessValidator userAccessValidator = new UserAccessValidator(identifierService, validationService,
				schemaService);
		String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		User user = TestHelper.mockUser();
		Assert.assertThrows(ForbiddenOperationException.class, () -> {
			userAccessValidator.canUserCreateIGSNRecord(xml, user);
		});
	}

}