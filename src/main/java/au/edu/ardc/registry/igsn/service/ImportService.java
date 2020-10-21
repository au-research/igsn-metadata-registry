package au.edu.ardc.registry.igsn.service;

import au.edu.ardc.registry.common.entity.*;
import au.edu.ardc.registry.common.model.Attribute;
import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.provider.*;
import au.edu.ardc.registry.common.service.*;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.exception.VersionContentAlreadyExistsException;
import org.apache.logging.log4j.core.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class ImportService {

	@Autowired
	private IdentifierService identifierService;

	@Autowired
	private RecordService recordService;

	@Autowired
	private IGSNVersionService igsnVersionService;

	@Autowired
	private URLService urlService;

	@Autowired
	private SchemaService schemaService;

	@Autowired
	private IGSNRequestService igsnRequestService;

	public Identifier importRequest(File file, Request request) throws IOException {
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

		URL url = new URL();
		url.setRecord(record);
		url.setUrl(landingPage);
		url.setCreatedAt(request.getCreatedAt());
		urlService.create(url);
		requestLog.debug("Added URL: {}", url.getId());

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

	public Identifier updateRequest(File file, Request request) throws IOException, VersionContentAlreadyExistsException {
		Logger requestLog = igsnRequestService.getLoggerFor(request);
		String content = Helpers.readFile(file);
		String creatorID = request.getAttribute(Attribute.CREATOR_ID);

        Schema schema = schemaService.getSchemaForContent(content);

		IdentifierProvider identifierProvider = (IdentifierProvider) MetadataProviderFactory.create(schema,
				Metadata.Identifier);
        String identifierValue = identifierProvider.get(content);
		VisibilityProvider visibilityProvider = (VisibilityProvider) MetadataProviderFactory.create(schema,
				Metadata.Visibility);

        Identifier identifier = identifierService.findByValueAndType(identifierValue, Identifier.Type.IGSN);
        Record record = identifier.getRecord();
		Optional<Version> cVersion = record.getCurrentVersions().stream()
				.filter(version -> version.getSchema().equals(schema.getId())).findFirst();
		if (cVersion.isPresent()) {
			Version version = cVersion.get();
			String versionHash = version.getHash();
			String incomingHash = VersionService.getHash(content);
			if (incomingHash.equals(versionHash)) {
				throw new VersionContentAlreadyExistsException(identifierValue, version.getSchema());
			}
		}

        // update the record
        record.setVisible(visibilityProvider.get(content));
        record.setModifierID(UUID.fromString(creatorID));
        record.setModifiedAt(request.getCreatedAt());
        recordService.save(record);

        // end current version for the given schema
        Version currentVersion = igsnVersionService.getCurrentVersionForRecord(record, schema.getId());
        igsnVersionService.end(currentVersion, UUID.fromString(creatorID));

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

        // todo end all version and set latest version to current

        return identifier;
	}

}
