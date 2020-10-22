package au.edu.ardc.registry.igsn.service;

import au.edu.ardc.registry.common.entity.*;
import au.edu.ardc.registry.common.model.Attribute;
import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.provider.*;
import au.edu.ardc.registry.common.service.*;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.exception.ForbiddenOperationException;
import au.edu.ardc.registry.exception.VersionContentAlreadyExistsException;
import org.apache.logging.log4j.core.Logger;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Service
public class ImportService {

	private final IdentifierService identifierService;

	private final RecordService recordService;

	private final IGSNVersionService igsnVersionService;

	private final URLService urlService;

	private final SchemaService schemaService;

	private final IGSNRequestService igsnRequestService;

	public ImportService(IdentifierService identifierService, RecordService recordService,
			IGSNVersionService igsnVersionService, URLService urlService, SchemaService schemaService,
			IGSNRequestService igsnRequestService) {
		this.identifierService = identifierService;
		this.recordService = recordService;
		this.igsnVersionService = igsnVersionService;
		this.urlService = urlService;
		this.schemaService = schemaService;
		this.igsnRequestService = igsnRequestService;
	}

	/**
	 * Import (Ingest) a File for a Request
	 * @param file the {@link File} points to the payload or the chunked payload
	 * @param request the {@link Request} where additional details will be extracted from
	 * @return the {@link Identifier}
	 * @throws IOException when failing to read file or any other operation
	 */
	public Identifier importRequest(File file, Request request) throws IOException, ForbiddenOperationException {
		Logger requestLog = igsnRequestService.getLoggerFor(request);

		// read the content of the item Resource
		String content = Helpers.readFile(file);

		String creatorID = request.getAttribute(Attribute.CREATOR_ID);
		String allocationID = request.getAttribute(Attribute.ALLOCATION_ID);
		String ownerType = request.getAttribute(Attribute.OWNER_TYPE) != null
				? request.getAttribute(Attribute.OWNER_TYPE) : "User";

		// build the providers
		Schema schema = schemaService.getSchemaForContent(content);
		IdentifierProvider identifierProvider = (IdentifierProvider) MetadataProviderFactory.create(schema,
				Metadata.Identifier);
		VisibilityProvider visibilityProvider = (VisibilityProvider) MetadataProviderFactory.create(schema,
				Metadata.Visibility);
		LandingPageProvider landingPageProvider = (LandingPageProvider) MetadataProviderFactory.create(schema,
				Metadata.LandingPage);

		// obtain the necessary information from the providers
		String identifierValue = identifierProvider.get(content);
		String landingPage = landingPageProvider.get(content);

		requestLog.debug("Ingesting Identifier: {} with Landing Page: {}", identifierValue, landingPage);

		// add new Record, Identifier, URL and Version
		Record record = new Record();
		record.setCreatedAt(request.getCreatedAt());
		record.setModifiedAt(request.getCreatedAt());
		record.setOwnerID(UUID.fromString(creatorID));
		record.setOwnerType(Record.OwnerType.valueOf(ownerType));
		record.setVisible(visibilityProvider.get(content));
		record.setAllocationID(UUID.fromString(allocationID));
		record.setCreatorID(UUID.fromString(creatorID));
		record.setRequestID(request.getId());
		recordService.save(record);
		requestLog.debug("Added Record: {}", record.getId());

		Identifier identifier = new Identifier();
		identifier.setCreatedAt(request.getCreatedAt());
		identifier.setRecord(record);
		identifier.setType(Identifier.Type.IGSN);
		identifier.setValue(identifierValue);
		identifier.setRequestID(request.getId());
		identifier.setStatus(Identifier.Status.PENDING);
		try {
			identifierService.save(identifier);
		}
		catch (Exception e) {
			requestLog.error("Failed creating Identifier: {}", identifierValue);
			requestLog.error("Deleting created record: {}", record.getId());
			recordService.delete(record);
			return null;
		}
		requestLog.debug("Added Identifier: {}", identifier.getId());

		Version version = new Version();
		version.setRecord(record);
		version.setSchema(schema.getId());
		version.setContent(content.getBytes());
		version.setCreatorID(UUID.fromString(creatorID));
		version.setCreatedAt(request.getCreatedAt());
		version.setCurrent(true);
		version.setHash(VersionService.getHash(content));
		version.setRequestID(request.getId());
		igsnVersionService.save(version);
		requestLog.debug("Added version: {}", version.getId());

		// append identifierValue to the outputFile path for use in next step
		requestLog.info("Ingested {}", identifierValue);

		return identifier;
	}

	/**
	 * Update an existing IGSN Record. The workflow is different from
	 * {@link #importRequest(File, Request)}
	 * @param file the {@link File} that contains the new updated version
	 * @param request the {@link Request} that contains all additional parameters
	 * @return the IGSN {@link Identifier} that is updated
	 * @throws IOException when reading the file
	 * @throws VersionContentAlreadyExistsException when the exact same version is updated
	 */
	public Identifier updateRequest(File file, Request request) throws IOException, ForbiddenOperationException {
		Logger requestLog = igsnRequestService.getLoggerFor(request);
		requestLog.debug("Updating content for request:{} with file:{}", request, file.getAbsolutePath());

		String content = Helpers.readFile(file);
		String creatorID = request.getAttribute(Attribute.CREATOR_ID);
		Schema schema = schemaService.getSchemaForContent(content);

		IdentifierProvider identifierProvider = (IdentifierProvider) MetadataProviderFactory.create(schema,
				Metadata.Identifier);
		String identifierValue = identifierProvider.get(content);
		VisibilityProvider visibilityProvider = (VisibilityProvider) MetadataProviderFactory.create(schema,
				Metadata.Visibility);

		Identifier identifier = identifierService.findByValueAndType(identifierValue, Identifier.Type.IGSN);

		if (identifier == null) {
			throw new ForbiddenOperationException(String.format("Identifier with value %s and type %s doesn't exist",
					identifierValue, Identifier.Type.IGSN));
		}
		Record record = identifier.getRecord();
		if (record == null) {
			throw new ForbiddenOperationException(
					String.format("Record with Identifier %s doesn't exist", identifierValue));
		}
		Optional<Version> cVersion = record.getCurrentVersions().stream()
				.filter(version -> version.getSchema().equals(schema.getId())).findFirst();
		if (cVersion.isPresent()) {
			Version version = cVersion.get();
			String versionHash = version.getHash();
			String incomingHash = VersionService.getHash(content);
			if (incomingHash.equals(versionHash)) {
				requestLog.warn("Previous version already contain the same content. Skipping");
				throw new VersionContentAlreadyExistsException(identifierValue, version.getSchema());
			}
		}

		// update the record
		record.setVisible(visibilityProvider.get(content));
		record.setModifierID(UUID.fromString(creatorID));
		record.setModifiedAt(request.getCreatedAt());
		recordService.save(record);
		requestLog.debug("Updated record {}", record.getId());

		// end current version for the given schema
		Version currentVersion = igsnVersionService.getCurrentVersionForRecord(record, schema.getId());
		igsnVersionService.end(currentVersion, UUID.fromString(creatorID));
		requestLog.debug("Ended previous version of the schema {}", schema.getId());

		// create new current version
		Version version = new Version();
		version.setRecord(record);
		version.setSchema(schema.getId());
		version.setContent(content.getBytes());
		version.setCreatorID(UUID.fromString(creatorID));
		version.setCreatedAt(request.getCreatedAt());
		version.setCurrent(true);
		version.setHash(VersionService.getHash(content));
		igsnVersionService.save(version);
		requestLog.debug("Created new version {}", version.getId());

		// todo end all version and set latest version to current

		requestLog.info("Updated identifier {} with a new version", identifier.getValue());
		return identifier;
	}

}
