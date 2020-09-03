package au.edu.ardc.registry.igsn.validator;

import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.provider.IdentifierProvider;
import au.edu.ardc.registry.common.provider.Metadata;
import au.edu.ardc.registry.common.provider.MetadataProviderFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Payload Validator is the top level validator when records sent to the registry it
 * performs several validations Content validation to check if xml or json or csv is well
 * formed and valid User access validation checks if user has the desired access for the
 * allocations needed to complete the tasks Version Content checks is Record has a version
 * with the given schema and it is different from the current one
 */
public class PayloadValidator {

	@Autowired
	ContentValidator cValidator;

	@Autowired
	UserAccessValidator uaValidator;

	public boolean isvalidPayload(String content) throws Exception {
		boolean isValid = true;
		// Validate the entire content even if it contains multiple resources
		isValid = cValidator.validate(content);
		return isValid;
	}

	public boolean hasUserAccess(String content, User user) throws Exception {

		Schema schema = cValidator.service.getSchemaForContent(content);
		IdentifierProvider provider = (IdentifierProvider) MetadataProviderFactory.create(schema, Metadata.Identifier);
		assert provider != null;
		List<String> identifiers = provider.getAll(content);
		for (String identifier : identifiers) {
			if (!uaValidator.canCreateIdentifier(identifier, user)) {
				return false;
			}
		}
		return true;
	}

}
