package au.edu.ardc.registry.igsn.service;

import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.model.Attribute;
import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.model.Scope;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.provider.IdentifierProvider;
import au.edu.ardc.registry.common.provider.Metadata;
import au.edu.ardc.registry.common.provider.MetadataProviderFactory;
import au.edu.ardc.registry.common.service.IdentifierService;
import au.edu.ardc.registry.common.service.RecordService;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.service.ValidationService;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.exception.ContentNotSupportedException;
import au.edu.ardc.registry.exception.ForbiddenOperationException;
import au.edu.ardc.registry.exception.XMLValidationException;
import au.edu.ardc.registry.igsn.model.IGSNAllocation;
import au.edu.ardc.registry.igsn.model.IGSNTask;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Service for validating IGSN Requests Mainly
 */
@Service
public class IGSNRequestValidationService {

	final SchemaService schemaService;

	final IGSNService igsnService;

	final IdentifierService identifierService;

	final RecordService recordService;

	final ValidationService validationService;

	public IGSNRequestValidationService(SchemaService schemaService, IGSNService igsnService,
			IdentifierService identifierService, RecordService recordService, ValidationService validationService) {
		this.schemaService = schemaService;
		this.igsnService = igsnService;
		this.identifierService = identifierService;
		this.recordService = recordService;
		this.validationService = validationService;
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
	public void validate(@NotNull Request request, User user) throws IOException {

		File file = new File(request.getAttribute(Attribute.PAYLOAD_PATH));
		String content = Helpers.readFile(file);

		// todo validate request.attributes.ownerType, request.attributes.ownerID

		// todo validate reserve, transfer, bulk-reserve, bulk-transfer

		// todo single-mint, check for

		// validate well-formed and schema validation for all requests
		schemaService.validate(content);

		// get first IdentifierValue in the payload
		String type = request.getType();
		Scope scope = Scope.UPDATE;
		if (type.equals(IGSNService.EVENT_BULK_MINT) || (type.equals(IGSNService.EVENT_MINT))) {
			scope = Scope.CREATE;
		}
		IGSNAllocation allocation = igsnService.getIGSNAllocationForContent(content, user, scope);
		if (allocation == null) {
			// todo language
			request.setStatus(Request.Status.FAILED);
			throw new ForbiddenOperationException("You don't have access to this resource");
		}

		// get the first identifier and find existingIdentifier
		Schema schema = schemaService.getSchemaForContent(content);
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

		// if it's bulk, all identifiers has to be the same
		// no mix allocation
		if (type.equals(IGSNService.EVENT_BULK_MINT) || type.equals(IGSNService.EVENT_BULK_UPDATE)) {
			String prefix = allocation.getPrefix();
			String namespace = allocation.getNamespace();
			for (String identifier : identifiers) {
				if (!identifier.startsWith(prefix + "/" + namespace)) {
					// todo language
					request.setStatus(Request.Status.FAILED);
					throw new ForbiddenOperationException(
							identifier + " doesn't match previous Identifiers's prefix or namespace");
				}
			}
			return;
		}

		// BULK VALIDATION ENDS HERE

		// SINGLE MINT OR UPDATE CONTINUES HERE

		// get the first one, and start validating singles
		String firstIdentifier = identifiers.get(0);
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
