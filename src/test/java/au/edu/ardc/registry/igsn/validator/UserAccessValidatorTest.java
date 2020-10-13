package au.edu.ardc.registry.igsn.validator;

import au.edu.ardc.registry.TestHelper;
import au.edu.ardc.registry.common.dto.mapper.IdentifierMapper;
import au.edu.ardc.registry.common.dto.mapper.RecordMapper;
import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.model.Allocation;
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

import static org.assertj.core.api.Assertions.assertThat;

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

	@Test
	@DisplayName("Mis-match identifier values")
	void canUserCreateIGSNRecord_mixedIdentifier_ForbiddenOperationException() throws IOException {
		UserAccessValidator userAccessValidator = new UserAccessValidator(identifierService, validationService,
				schemaService);

		// xml with 10273/XXAB and 20.500.11812/XXAB
		String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1_mismatchPrefix.xml");
		User user = TestHelper.mockUser();
		IGSNAllocation allocation = TestHelper.mockIGSNAllocation();
		allocation.setPrefix("10273");
		allocation.setNamespace("XXAB");
		allocation.setScopes(Arrays.asList(Scope.CREATE, Scope.UPDATE));
		user.setAllocations(Collections.singletonList(allocation));

		Assert.assertThrows(ForbiddenOperationException.class, () -> {
			userAccessValidator.canUserCreateIGSNRecord(xml, user);
		});
	}

	@Test
	@DisplayName("User can create identifier but Identifier with the same value and type already exists")
	void canUserCreateIGSNRecord_recordAlreadyExists_ForbiddenOperationException() throws IOException {
		UserAccessValidator userAccessValidator = new UserAccessValidator(identifierService, validationService,
				schemaService);

		// xml with 10273/XX0T, allocation with the same set, user associated with
		String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		User user = TestHelper.mockUser();
		IGSNAllocation allocation = TestHelper.mockIGSNAllocation();
		allocation.setPrefix("10273");
		allocation.setNamespace("XX0T");
		allocation.setScopes(Arrays.asList(Scope.CREATE, Scope.UPDATE));
		user.setAllocations(Collections.singletonList(allocation));

		// identifier already exists in identifierRepository
		Identifier mockedIdentifier = TestHelper.mockIdentifier();
		mockedIdentifier.setValue("10273/XX0TUIAYLV");
		mockedIdentifier.setType(Identifier.Type.IGSN);
		Mockito.when(identifierRepository.findFirstByValueAndType("10273/XX0TUIAYLV", Identifier.Type.IGSN))
				.thenReturn(mockedIdentifier);

		Assert.assertThrows(ForbiddenOperationException.class, () -> {
			userAccessValidator.canUserCreateIGSNRecord(xml, user);
		});
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
	@DisplayName("Update operation, Record doesn't exist, forbidden operation")
	void canUserUpdateIGSNRecord_RecordDoesntExist_ForbiddenOperation() throws IOException {
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

		Mockito.when(identifierRepository.findFirstByValueAndType("10273/XX0TUIAYLV", Identifier.Type.IGSN))
				.thenReturn(null);

		Assert.assertThrows(ForbiddenOperationException.class, () -> {
			userAccessValidator.canUserUpdateIGSNRecord(xml, user);
		});
	}

	@Test
	@DisplayName("Update operation, Record exists but User doesn't own the record, forbidden operation")
	void canUserUpdateIGSNRecord_UserHasNoAccessToRecord_ForbiddenOperation() throws IOException {
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

		// record exists with the same allocation ID, but the owner is another random user
		Record mockRecord = TestHelper.mockRecord(UUID.randomUUID());
		mockRecord.setAllocationID(allocation.getId());
		mockRecord.setOwnerType(Record.OwnerType.User);
		mockRecord.setOwnerID(UUID.randomUUID());

		// identifier exists in identifierRepository, and linked to the mockedRecord
		Identifier mockedIdentifier = TestHelper.mockIdentifier();
		mockedIdentifier.setValue("10273/XX0TUIAYLV");
		mockedIdentifier.setType(Identifier.Type.IGSN);
		mockedIdentifier.setRecord(mockRecord);
		Mockito.when(identifierRepository.findFirstByValueAndType("10273/XX0TUIAYLV", Identifier.Type.IGSN))
				.thenReturn(mockedIdentifier);

		Assert.assertThrows(ForbiddenOperationException.class, () -> {
			userAccessValidator.canUserUpdateIGSNRecord(xml, user);
		});
	}

	@Test
	public void testUserAccessToIdentifier() {
		String identifier = "20.500.11812/XXAASSSSIIIIUUUU";
		User user = TestHelper.mockUser();
		user.setAllocations(Collections.singletonList(TestHelper.mockIGSNAllocation()));
		for (Allocation allocation : user.getAllocations()) {
			if (allocation.getType().equals("urn:ardc:igsn:allocation")) {
				String prefix = ((IGSNAllocation) allocation).getPrefix();
				assertThat(prefix.equals("20.500.11812")).isTrue();
				String namespace = ((IGSNAllocation) allocation).getNamespace();
				assertThat(namespace.equals("XXAA")).isTrue();
				assertThat(identifier.startsWith(prefix + "/" + namespace)).isTrue();
			}
		}
	}

}