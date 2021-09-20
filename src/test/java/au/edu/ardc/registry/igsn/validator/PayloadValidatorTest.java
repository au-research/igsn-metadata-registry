package au.edu.ardc.registry.igsn.validator;

import au.edu.ardc.registry.TestHelper;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.service.IdentifierService;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.service.ValidationService;
import au.edu.ardc.registry.common.service.VersionService;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.exception.ForbiddenOperationException;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.EnabledIf;

import java.io.IOException;

@SpringBootTest
class PayloadValidatorTest {

	@Autowired
	SchemaService schemaService;

	@Autowired
	ValidationService validationService;

	@Autowired
	IdentifierService identifierService;

	@Autowired
	VersionService versionService;

	@Test
	@DisplayName("User has no access to the identifier")
	void validateMintPayload() throws IOException {

		// proper ardcv1
		String payload = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		User user = TestHelper.mockUser(); // mocked user with no permission

		PayloadValidator validator = new PayloadValidator(schemaService, validationService, identifierService,
				versionService);

		// mocked user has no access to the identifier in the sample,
		// result in a ForbiddenOperationException
		Assert.assertThrows(ForbiddenOperationException.class, () -> validator.validateMintPayload(payload, user));
	}

}