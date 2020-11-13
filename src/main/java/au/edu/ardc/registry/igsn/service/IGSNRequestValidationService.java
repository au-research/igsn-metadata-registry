package au.edu.ardc.registry.igsn.service;

import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.model.*;
import au.edu.ardc.registry.common.provider.IdentifierProvider;
import au.edu.ardc.registry.common.provider.Metadata;
import au.edu.ardc.registry.common.provider.MetadataProviderFactory;
import au.edu.ardc.registry.common.service.*;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.exception.ContentNotSupportedException;
import au.edu.ardc.registry.exception.ForbiddenOperationException;
import au.edu.ardc.registry.exception.XMLValidationException;
import au.edu.ardc.registry.igsn.model.IGSNAllocation;
import au.edu.ardc.registry.igsn.model.IGSNTask;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service for validating IGSN Requests Mainly
 */
@Service
@ConditionalOnProperty(name = "app.igsn.enabled")
public class IGSNRequestValidationService {

	final SchemaService schemaService;

	final IGSNService igsnService;

	final IdentifierService identifierService;

	final RecordService recordService;

	final ValidationService validationService;

	final KeycloakService keycloakService;

	final List<String> supportedOwnerTypes = Arrays.asList("User", "DataCenter");

	public IGSNRequestValidationService(SchemaService schemaService, IGSNService igsnService,
			IdentifierService identifierService, RecordService recordService, ValidationService validationService, KeycloakService keycloakService) {
		this.schemaService = schemaService;
		this.igsnService = igsnService;
		this.identifierService = identifierService;
		this.recordService = recordService;
		this.validationService = validationService;
		this.keycloakService = keycloakService;
	}

	/**
	 * Validates a Request. Will not throw Exception
	 * @param request the {@link Request} that is pre-populated with required Attributes
	 * and Payload
	 * @param user the {@link User} that initiate this Request
	 * @throws IOException when the payload is not readable
	 * @throws ForbiddenOperationException when the Operation is not allowed due to
	 * validation logic
	 * @throws XMLValidationException when the payload content failed XML Validation
	 * @throws ContentNotSupportedException when the payload content type is not supported
	 */
	public void validate(@NotNull Request request, User user) throws IOException ,ContentNotSupportedException{
		String type = request.getType();
		// check for file size before anything else
		// 5 MB for BATCH
		long maxContentSize = 5 * 1024 * 1024;
		// 60 KB for single mint and update
		if(type.equals(IGSNService.EVENT_MINT) || type.equals(IGSNService.EVENT_UPDATE)){
			maxContentSize = 60 * 1024; // 60 KB
		}

		File file = new File(request.getAttribute(Attribute.PAYLOAD_PATH));

		Helpers.checkFileSize(request.getAttribute(Attribute.PAYLOAD_PATH), maxContentSize);


		String content = Helpers.readFile(file);

		// validate well-formed and schema validation for all requests

		Schema schema = null;
		if(request.getAttribute(Attribute.SCHEMA_ID) != null){
			schema = schemaService.getSchemaByID(request.getAttribute(Attribute.SCHEMA_ID));
			if(schema == null){
				throw new ContentNotSupportedException(String.format("Validator for schema %s is not found",
						request.getAttribute(Attribute.SCHEMA_ID)));
			}
		}else{
			schema = schemaService.getSchemaForContent(content);
		}

		schemaService.validate(content);

		// get first IdentifierValue in the payload

		Scope scope = Scope.UPDATE;
		if (type.equals(IGSNService.EVENT_BULK_MINT) || (type.equals(IGSNService.EVENT_MINT))
		|| type.equals(IGSNService.EVENT_RESERVE)) {
			scope = Scope.CREATE;
		}

		// get the first identifier and find existingIdentifier


		IdentifierProvider provider = (IdentifierProvider) MetadataProviderFactory.create(schema, Metadata.Identifier);
		List<String> identifiers = provider.getAll(content);

		if(identifiers.isEmpty()){
			request.setStatus(Request.Status.FAILED);
			throw new ContentNotSupportedException("Unable to fetch Identifiers for given content");
		}else if(identifiers.size() > 1 &&
				(type.equals(IGSNService.EVENT_MINT) || type.equals(IGSNService.EVENT_UPDATE))){
			request.setStatus(Request.Status.FAILED);
			throw new ContentNotSupportedException(String.format("Only single resource is allowed for %s service", type));
		}

		String firstIdentifier = identifiers.get(0);

		IGSNAllocation allocation = igsnService.getIGSNAllocationForIdentifier(firstIdentifier, user, scope);

		if (allocation == null) {
			// todo language
			request.setStatus(Request.Status.FAILED);
			throw new ForbiddenOperationException(String.format("User has no access to the given Identifier: %s", firstIdentifier));
		}

		request.setAttribute(Attribute.ALLOCATION_ID, allocation.getId().toString());

		/*
		User currently only able to mint, reserve , transfer as a datacenter they are member of
		updates does not have ownerID
		*/
		if(type.equals(IGSNService.EVENT_BULK_MINT) || type.equals(IGSNService.EVENT_MINT)
				||  type.equals(IGSNService.EVENT_RESERVE) || type.equals(IGSNService.EVENT_TRANSFER)){
			// check for supported ownerType
			// Owner type must be User or DataCenter
			String ownerType = request.getAttribute(Attribute.OWNER_TYPE);
			if(!supportedOwnerTypes.contains(ownerType)){
				throw new ForbiddenOperationException(String.format("OwnerType value: %s is not supported", ownerType));
			}

			UUID ownerID = UUID.fromString(request.getAttribute(Attribute.OWNER_ID));
			if(!ownerID.equals(user.getId())){
				if(!ownerType.equals("DataCenter")){
					throw new ForbiddenOperationException(String.format("User can only create records for a datacenter not: %s", ownerType));
				}
				// validate the owen not just the user
				// validate reserve, transfer, bulk-reserve, bulk-transfer
				boolean userHasDataCenter = false;
				List<DataCenter> dataCenters = user.getDataCenters();
				for (DataCenter dataCenter : dataCenters) {
					if (dataCenter.getId().equals(ownerID)) {
						userHasDataCenter = true;
						break;
					}
				}
				if(!userHasDataCenter){
					throw new ForbiddenOperationException(String.format("User is not a member of datacenter : %s", ownerID));
				}
			}
		}




		// if it's bulk, all identifiers has to be the same
		// no mix allocation
		if (type.equals(IGSNService.EVENT_BULK_MINT) || type.equals(IGSNService.EVENT_BULK_UPDATE)) {
			String prefix = allocation.getPrefix();
			String namespace = allocation.getNamespace();
			for (String identifierValue : identifiers) {
				if (!identifierValue.startsWith(prefix + "/" + namespace)) {
					// todo language
					request.setStatus(Request.Status.FAILED);
					throw new ForbiddenOperationException(String.format("Mixed allocations are not supported. %s " +
							"doesn't match the prefix or namespace of the previous identifier. ", identifierValue));
				}
			}
			return;
		}

		// BULK VALIDATION ENDS HERE

		// SINGLE MINT OR UPDATE CONTINUES HERE

		// get the first one, and start validating singles

		Identifier existingIdentifier = identifierService.findByValueAndType(firstIdentifier, Identifier.Type.IGSN);

		// if it's single mint, check if the identifier already exist
		if (existingIdentifier != null && type.equals(IGSNService.EVENT_MINT)) {
			request.setStatus(Request.Status.FAILED);
			throw new ForbiddenOperationException(
					String.format("Record already exist with Identifier %s", firstIdentifier));
		}

		// if it's a single mint, check if the Identifier is already being queued to be
		// imported
		if (type.equals(IGSNService.EVENT_MINT)
				&& igsnService.hasIGSNTaskQueued(allocation.getId(), IGSNTask.TASK_IMPORT, firstIdentifier)) {
			request.setStatus(Request.Status.FAILED);
			throw new ForbiddenOperationException(
					String.format("Identifier %s is already queued to be minted", firstIdentifier));
		}

		// if it's a single update, check if the identifier doesn't exist and if the user
		// owns it
		if (type.equals(IGSNService.EVENT_UPDATE)) {
			if (existingIdentifier == null) {
				request.setStatus(Request.Status.FAILED);
				throw new ForbiddenOperationException(
						String.format("Record doesn't exist with Identifier %s", firstIdentifier));
			}
			else {
				Record record = existingIdentifier.getRecord();
				if (!validationService.validateRecordOwnership(record, user)) {
					request.setStatus(Request.Status.FAILED);
					throw new ForbiddenOperationException("User has no access to the Record: " + record.getId());
				}
			}
		}
	}
}
