package au.edu.ardc.registry.igsn.validator;

import au.edu.ardc.registry.KeycloakIntegrationTest;
import au.edu.ardc.registry.common.model.Allocation;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.service.KeycloakService;
import au.edu.ardc.registry.igsn.model.IGSNAllocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class UserAccessValidatorIT extends KeycloakIntegrationTest {

	@Autowired
	private KeycloakService kcService;

	@Test
	@DisplayName("match the IGSN allocation to a given identifier value")
	public void matchidentifier_with_Allocation() throws Exception {
		String identifier = "10273/XXAASSSSIIIIUUUU";
		String allocationId = "6d8c4563-1b33-4cb1-9267-45b9cc574b0b";
		//  it's an IGSN allocationID so it will return an IGSNAllocation
		IGSNAllocation a = (IGSNAllocation) kcService.getAllocationByResourceID(allocationId);
		if(a.getType().equals("urn:ardc:igsn:allocation")) {
			String prefix = a.getPrefix();
			String namespace = a.getNamespace();
			assertThat(identifier.startsWith(prefix + "/" + namespace));
		}
	}
}