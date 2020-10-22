package au.edu.ardc.registry.job.processor;

import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.common.model.Attribute;
import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.provider.*;
import au.edu.ardc.registry.common.service.*;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.exception.ContentProviderNotFoundException;
import au.edu.ardc.registry.igsn.service.IGSNRequestService;
import au.edu.ardc.registry.igsn.service.IGSNVersionService;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

public class UpdateRecordProcessor implements ItemProcessor<Resource, Resource> {

	private final IGSNRequestService igsnRequestService;

	private final SchemaService schemaService;

	private final IdentifierService identifierService;

	private final RecordService recordService;

	private final IGSNVersionService igsnVersionService;

	private final URLService urlService;

	private String creatorID;

	private String outputFilePath;

	private String allocationID;

	private String ownerType;

	private Schema schema;

	public UpdateRecordProcessor(SchemaService schemaService, IdentifierService identifierService,
			RecordService recordService, IGSNVersionService versionService, URLService urlService,
			IGSNRequestService igsnRequestService) {

		this.identifierService = identifierService;
		this.recordService = recordService;
		this.igsnVersionService = versionService;
		this.urlService = urlService;
		this.schemaService = schemaService;
		this.igsnRequestService = igsnRequestService;
	}

	@BeforeStep
	public void beforeStep(final StepExecution stepExecution) {
		JobParameters jobParameters = stepExecution.getJobParameters();
		String requestID = jobParameters.getString("IGSNServiceRequestID");
		Request request = igsnRequestService.findById(requestID);

		this.creatorID = request.getAttribute(Attribute.CREATOR_ID);
		this.outputFilePath = request.getAttribute(Attribute.REQUESTED_IDENTIFIERS_PATH);
		this.allocationID = request.getAttribute(Attribute.ALLOCATION_ID);
		this.ownerType = request.getAttribute(Attribute.OWNER_TYPE);
	}

	@Override
	public Resource process(Resource item) throws Exception {
		processContent(item);
		return null;
	}

	private void processContent(Resource item) throws IOException, ContentProviderNotFoundException {
		String content = Helpers.readFile(item.getFile().getPath());
		schema = schemaService.getSchemaForContent(content);
		IdentifierProvider identifierProvider = (IdentifierProvider) MetadataProviderFactory.create(schema,
				Metadata.Identifier);
		String identifierValue = identifierProvider.get(content);
		LandingPageProvider landingPageProvider = (LandingPageProvider) MetadataProviderFactory.create(schema,
				Metadata.LandingPage);
		String landingPage = landingPageProvider.get(content);
		Identifier existingIdentifier = identifierService.findByValueAndType(identifierValue, Identifier.Type.IGSN);
		VisibilityProvider visibilityProvider = (VisibilityProvider) MetadataProviderFactory.create(schema,
				Metadata.Visibility);

		Record record = existingIdentifier.getRecord();
		record.setVisible(visibilityProvider.get(content));
		record.setModifierID(UUID.fromString(creatorID));
		record.setModifiedAt(new Date());
		// end current version for the given schema
		Version currentVersion = igsnVersionService.getCurrentVersionForRecord(record, schema.getId());
		igsnVersionService.end(currentVersion, UUID.fromString(creatorID));
		// add new version
		addNewVersion(content, record);
		recordService.save(record);
		// append the identifier to the text file for minting IGSN prcessor use
		Helpers.appendToFile(outputFilePath, identifierValue);
	}

	private void addNewVersion(String content, Record record) {
		Version version = new Version();
		version.setRecord(record);
		version.setSchema(schema.getId());
		version.setContent(content.getBytes());
		version.setCreatorID(UUID.fromString(creatorID));
		version.setCreatedAt(new Date());
		version.setCurrent(true);
		version.setHash(VersionService.getHash(content));
		igsnVersionService.save(version);
	}

}
