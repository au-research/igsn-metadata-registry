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

	@Autowired
	VersionContentValidator vcValidator;

	/**
	 * @param content String the payload content as String
	 * @param user the logged in User who requested the mint / update
	 * @return true if the content can be processed or false if errors or access is denied to user
	 * @throws Exception
	 */
	public boolean validaPayload(String content, User user) throws Exception {
		// validate the entire XML or JSON content
		boolean isValidContent = cValidator.validate(content);
		if(isValidContent){
			// check if the current user has insert or update access for the records with the given identifiers
			boolean hasUserAccess =	uaValidator.hasUserAccess(content, user);
			if(hasUserAccess){
				// check if the contents are new compared what stored in the registry
				return vcValidator.isNewContent(content);
			}
		}
		return false;
	}
}
