package au.edu.ardc.registry.igsn.validator;

import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.model.Allocation;
import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.model.Scope;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.provider.IdentifierProvider;
import au.edu.ardc.registry.common.provider.Metadata;
import au.edu.ardc.registry.common.provider.MetadataProviderFactory;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.service.ValidationService;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.exception.ContentNotSupportedException;
import au.edu.ardc.registry.exception.ForbiddenOperationException;
import au.edu.ardc.registry.igsn.model.IGSNAllocation;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserAccessValidator {

	@Autowired
	ValidationService vService;

	@Autowired
	SchemaService sService;

	private final String IGSNallocationType = "urn:ardc:igsn:allocation";

	/**
	 * @param content the entire payload XML or JSON
	 * @param user the user making the mint / update request
	 * @return true if user has access to all identifiers in the given payload
	 * @throws Exception
	 */
	public boolean hasUserAccess(String content, User user)
			throws ContentNotSupportedException, ForbiddenOperationException {
		Schema schema = sService.getSchemaForContent(content);
		IdentifierProvider provider = (IdentifierProvider) MetadataProviderFactory.create(schema, Metadata.Identifier);
		assert provider != null;
		List<String> identifiers = provider.getAll(content);
		for (String identifier : identifiers) {
			if (!this.canCreateIdentifier(identifier, user)) {
				throw new ForbiddenOperationException("User has no access to the given Identifier:" + identifier);
			}
		}
		return true;
	}


	/**
	 * @param Identifier an IGSN identifier
	 * @param user and IGSN User
	 * @return IGSNAllocation for the given identifier if the user has access to
	 */
	public IGSNAllocation getIGSNAllocation(String Identifier, User user) {
		// get allocation from identifier that user has access to
		List<Allocation> allocations = user.getAllocationsByType(this.IGSNallocationType);
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
		List<Allocation> allocations = user.getAllocationsByType(this.IGSNallocationType);
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
