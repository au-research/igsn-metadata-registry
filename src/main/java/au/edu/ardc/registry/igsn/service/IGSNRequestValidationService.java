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
import au.edu.ardc.registry.exception.ForbiddenOperationException;
import au.edu.ardc.registry.igsn.model.IGSNAllocation;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;

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

	public void validate(Request request, User user) throws IOException {

		File file = new File(request.getAttribute(Attribute.PAYLOAD_PATH));
		String content = Helpers.readFile(file);

		// todo validate reserve, transfer, bulk-reserve, bulk-transfer

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
			throw new ForbiddenOperationException("You don't have access to this resource");
		}

		// get the first identifier and find existingIdentifier
		Schema schema = schemaService.getSchemaForContent(content);
		IdentifierProvider provider = (IdentifierProvider) MetadataProviderFactory.create(schema, Metadata.Identifier);
		List<String> identifiers = provider.getAll(content);

		// if it's bulk, all identifiers has to be the same
		// no mix allocation
		if (type.equals(IGSNService.EVENT_BULK_MINT) || type.equals(IGSNService.EVENT_BULK_UPDATE)) {
			String prefix = allocation.getPrefix();
			String namespace = allocation.getNamespace();
			for (String identifier : identifiers) {
				if (!identifier.startsWith(prefix + "/" + namespace)) {
					// todo language
					throw new ForbiddenOperationException(
							identifier + " doesn't match previous Identifiers's prefix or namespace");
				}
			}
			return;
		}

		// get the first one, and start validating singles
		String firstIdentifier = identifiers.get(0);
		Identifier existingIdentifier = identifierService.findByValueAndType(firstIdentifier, Identifier.Type.IGSN);

		// if it's single mint, check if the identifier already exist
		if (existingIdentifier != null && type.equals(IGSNService.EVENT_MINT)) {
			throw new ForbiddenOperationException(
					String.format("Record already exist with Identifier %s", firstIdentifier));
		}

		// if it's a single update, check if the identifier doesn't exist and if the user
		// owns it
		if (type.equals(IGSNService.EVENT_UPDATE)) {
			if (existingIdentifier == null) {
				throw new ForbiddenOperationException(
						String.format("Record doesn't exist with Identifier %s", firstIdentifier));
			}
			else {
				Record record = existingIdentifier.getRecord();
				if (!validationService.validateRecordOwnership(record, user)) {
					throw new ForbiddenOperationException("User has no access to the Record: " + record.getId());
				}
			}
		}
	}

}
