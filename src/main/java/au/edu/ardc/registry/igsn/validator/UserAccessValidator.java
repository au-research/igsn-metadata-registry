package au.edu.ardc.registry.igsn.validator;

import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.model.Allocation;
import au.edu.ardc.registry.common.model.Scope;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.service.ValidationService;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.igsn.model.IGSNAllocation;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserAccessValidator {

	@Autowired
	ValidationService vService;

	/**
	 * @param Identifier an IGSN identifier
	 * @param user and IGSN User
	 * @return IGSNAllocation if the user has access to
	 */
	public IGSNAllocation getIGSNAllocation(String Identifier, User user) {
		// get allocation from identifier that user has access to
		List<Allocation> allocations = user.getAllocationsByType("urn:ardc:igsn:allocation");
		for (Allocation allocation : allocations) {
			IGSNAllocation ia = (IGSNAllocation) allocation;
			String prefix = ia.getPrefix();
			String namespace = ia.getNamespace();
			if(Identifier.startsWith(prefix + "/" + namespace)){
				return ia;
			}
		}
		return null;
	}


	/**
	 * @param identifier an IGSN identifier value
	 * @param user and IGSN User
	 * @return true if the user can create the Identifier
	 */
	public boolean canCreateIdentifier(String identifier, User user) {
		// get allocation from identifier that user has access to
		List<Allocation> allocations = user.getAllocationsByType("urn:ardc:igsn:allocation");
		for (Allocation allocation : allocations) {
			IGSNAllocation ia = (IGSNAllocation) allocation;
			String prefix = ia.getPrefix();
			String namespace = ia.getNamespace();
			if(identifier.startsWith(prefix + "/" + namespace)){
				return true;
			}
		}
		return false;
	}

	public boolean hasAccessToRecord(Record record, User user) throws Exception {
		return vService.validateRecordOwnership(record, user);
	}

	public boolean canCreate(IGSNAllocation a, User user) throws Exception {
		return vService.validateAllocationScope(a, user, Scope.CREATE);
	}

	public boolean canUpdate(IGSNAllocation a, User user) throws Exception {
		return vService.validateAllocationScope(a, user, Scope.UPDATE);
	}

}
