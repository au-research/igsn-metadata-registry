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
import au.edu.ardc.registry.exception.ContentNotSupportedException;
import au.edu.ardc.registry.exception.ForbiddenOperationException;
import au.edu.ardc.registry.igsn.model.IGSNAllocation;

import java.util.List;
import java.util.UUID;

public class UserAccessValidator {

	private final ValidationService validationService;

	private final SchemaService schemaService;

	private final IdentifierRepository identifierRepository;

	private final String IGSNallocationType = "urn:ardc:igsn:allocation";

	private UUID allocationID;

	public UserAccessValidator(IdentifierRepository identifierRepository, ValidationService validationService,
			SchemaService schemaService) {
		this.identifierRepository = identifierRepository;
		this.schemaService = schemaService;
		this.validationService = validationService;
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
		Schema schema = schemaService.getSchemaForContent(content);
		IdentifierProvider provider = (IdentifierProvider) MetadataProviderFactory.create(schema, Metadata.Identifier);
		assert provider != null;
		List<String> identifiers = provider.getAll(content);
		IGSNAllocation igsnAllocation = null;
		String prefix = "######";
		String namespace = "######";
		for (String identifierValue : identifiers) {
			if (identifierRepository.existsByTypeAndValue(Identifier.Type.IGSN, identifierValue)) {
				throw new ForbiddenOperationException("Record already exists with identifier: " + identifierValue);
			}
			if (igsnAllocation == null) {
				// get the first record and make sure all other records has the same
				// prefix and namespace
				igsnAllocation = getIGSNAllocation(identifierValue, user, Scope.CREATE);
				if (igsnAllocation == null) {
					throw new ForbiddenOperationException(
							"User has no access to the given Identifier: " + identifierValue);
				}
				allocationID = igsnAllocation.getId();
				prefix = igsnAllocation.getPrefix();
				namespace = igsnAllocation.getNamespace();
			}
			else if (!identifierValue.startsWith(prefix + "/" + namespace)) {
				throw new ForbiddenOperationException(
						"Identifier prefix is different from previous: " + identifierValue);
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
		Schema schema = schemaService.getSchemaForContent(content);
		IdentifierProvider provider = (IdentifierProvider) MetadataProviderFactory.create(schema, Metadata.Identifier);
		assert provider != null;
		List<String> identifiers = provider.getAll(content);
		for (String identifierValue : identifiers) {
			// if record doesn't exist user can't update it
			if (!identifierRepository.existsByTypeAndValue(Identifier.Type.IGSN, identifierValue)) {
				throw new ForbiddenOperationException("Record doesn't exists with identifier: " + identifierValue);
			}
			Identifier existingIdentifier = identifierRepository.findFirstByValueAndType(identifierValue,
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
	 * @param scope {create | update}
	 * @return IGSNAllocation for the given identifier if the user has access to
	 */

	public IGSNAllocation getIGSNAllocation(String identifierValue, User user, Scope scope) {
		// get allocation from identifier that user has access to
		List<Allocation> allocations = user.getAllocationsByType(this.IGSNallocationType);
		for (Allocation allocation : allocations) {
			IGSNAllocation ia = (IGSNAllocation) allocation;
			String prefix = ia.getPrefix();
			String namespace = ia.getNamespace();
			if (identifierValue.startsWith(prefix + "/" + namespace) && ia.getScopes().contains(scope)) {
				return ia;
			}
		}
		return null;
	}

	public UUID getAllocationID() {
		return allocationID;
	}

	public boolean hasAccessToRecord(Record record, User user) {
		return validationService.validateRecordOwnership(record, user);
	}

	public boolean canCreate(IGSNAllocation a, User user) {
		return validationService.validateAllocationScope(a, user, Scope.CREATE);
	}

	public boolean canUpdate(IGSNAllocation a, User user) {
		return validationService.validateAllocationScope(a, user, Scope.UPDATE);
	}

}
