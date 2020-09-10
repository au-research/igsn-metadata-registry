package au.edu.ardc.registry.igsn.validator;

import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.model.Allocation;
import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.model.Scope;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.provider.IdentifierProvider;
import au.edu.ardc.registry.common.provider.Metadata;
import au.edu.ardc.registry.common.provider.MetadataProviderFactory;
import au.edu.ardc.registry.common.repository.IdentifierRepository;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.service.ValidationService;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.exception.ContentNotSupportedException;
import au.edu.ardc.registry.exception.ForbiddenOperationException;
import au.edu.ardc.registry.igsn.model.IGSNAllocation;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.Cacheable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserAccessValidator {

	private final ValidationService vService;

	private final SchemaService sService;

	private final IdentifierRepository identifierRepository;

	private final String IGSNallocationType = "urn:ardc:igsn:allocation";

	public UserAccessValidator(IdentifierRepository identifierRepository, ValidationService vService,
			SchemaService sService) {
		this.identifierRepository = identifierRepository;
		this.sService = sService;
		this.vService = vService;
	}

	/**
	 * Tests for each IGSN Identifier in the content test for existing Identifiers tests
	 * if user has access to allocations with the given identifier prefix and namespace
	 * @param content the entire payload XML or JSON
	 * @param user the user making the mint / update request
	 * @return true if user has access to all identifiers in the given payload
	 * @throws ContentNotSupportedException when content is not supported
	 * @throws ForbiddenOperationException when user has no access to the identifier
	 */
	public boolean canUserCreateIGSNRecord(String content, User user)
			throws ContentNotSupportedException, ForbiddenOperationException {
		Schema schema = sService.getSchemaForContent(content);
		IdentifierProvider provider = (IdentifierProvider) MetadataProviderFactory.create(schema, Metadata.Identifier);
		assert provider != null;
		List<String> identifiers = provider.getAll(content);
		for (String identifierValue : identifiers) {
			if (!this.canCreateIdentifier(identifierValue, user)) {
				throw new ForbiddenOperationException("User has no access to the given Identifier: " + identifierValue);
			}
		}
		return true;
	}

	/**
	 * Tests for each IGSN Identifier in the content finds the records in the registry
	 * with eaach Identifier and checks if user has access to the records
	 * @param content the entire payload XML or JSON
	 * @param user the user making the mint / update request
	 * @return true if user has access to all Records (via their Identifier) contained in
	 * the payload
	 * @throws ContentNotSupportedException when content is not supported
	 * @throws ForbiddenOperationException when user has no access to the identifier
	 */
	public boolean canUserUpdateIGSNRecord(String content, User user)
			throws ContentNotSupportedException, ForbiddenOperationException {
		Schema schema = sService.getSchemaForContent(content);
		IdentifierProvider provider = (IdentifierProvider) MetadataProviderFactory.create(schema, Metadata.Identifier);
		assert provider != null;
		List<String> identifiers = provider.getAll(content);
		for (String identifierValue : identifiers) {
			// if record doesn't exist user can't update it
			if (!identifierRepository.existsByTypeAndValue(Identifier.Type.IGSN, identifierValue)) {
				throw new ForbiddenOperationException("Record doesn't exists with identifier: " + identifierValue);
			}
			Identifier existingIdentifier = identifierRepository.findByValueAndType(identifierValue,
					Identifier.Type.IGSN);
			Record record = existingIdentifier.getRecord();
			this.hasAccessToRecord(record, user);
			if (!this.hasAccessToRecord(record, user)) {
				throw new ForbiddenOperationException("User has no access to t Record: " + record.getId());
			}
		}
		return true;
	}

	/**
	 * Finds the allocation that User has access to to operate with the given Identifier
	 * space
	 * @param identifierValue an IGSN identifier
	 * @param user and IGSN User
	 * @return IGSNAllocation for the given identifier if the user has access to
	 */

	public IGSNAllocation getIGSNAllocation(String identifierValue, User user) {
		// get allocation from identifier that user has access to
		List<Allocation> allocations = user.getAllocationsByType(this.IGSNallocationType);
		for (Allocation allocation : allocations) {
			IGSNAllocation ia = (IGSNAllocation) allocation;
			String prefix = ia.getPrefix();
			String namespace = ia.getNamespace();
			if (identifierValue.startsWith(prefix + "/" + namespace)) {
				return ia;
			}
		}
		return null;
	}

	/**
	 * Tests if user has create scope
	 * @param identifierValue an IGSN identifier value
	 * @param user and IGSN User
	 * @return true if the user can create the Identifier
	 */
	public boolean canCreateIdentifier(String identifierValue, User user) {
		// If record already exists with the given Identifier then false
		if (identifierRepository.existsByTypeAndValue(Identifier.Type.IGSN, identifierValue)) {
			return false;
		}
		// get allocation from identifier that user has access to
		List<Allocation> allocations = user.getAllocationsByType(this.IGSNallocationType);
		for (Allocation allocation : allocations) {
			IGSNAllocation ia = (IGSNAllocation) allocation;
			String prefix = ia.getPrefix();
			String namespace = ia.getNamespace();
			if (identifierValue.startsWith(prefix + "/" + namespace) && ia.getScopes().contains(Scope.CREATE)) {
				return true;
			}
		}
		return false;
	}

	public boolean hasAccessToRecord(Record record, User user) {
		return vService.validateRecordOwnership(record, user);
	}

	public boolean canCreate(IGSNAllocation a, User user) {
		return vService.validateAllocationScope(a, user, Scope.CREATE);
	}

	public boolean canUpdate(IGSNAllocation a, User user) {
		return vService.validateAllocationScope(a, user, Scope.UPDATE);
	}

}
