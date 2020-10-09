package au.edu.ardc.registry.job.processor;

import au.edu.ardc.registry.common.entity.*;
import au.edu.ardc.registry.common.model.Attribute;
import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.provider.*;
import au.edu.ardc.registry.common.service.*;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.exception.ContentProviderNotFoundException;
import au.edu.ardc.registry.igsn.service.IGSNRequestService;
import au.edu.ardc.registry.igsn.service.IGSNVersionService;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

public class IngestRecordProcessor implements ItemProcessor<Resource, Resource> {

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

	public IngestRecordProcessor(SchemaService schemaService, IdentifierService identifierService,
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
	public Resource process(@NotNull Resource item) throws IOException, ContentProviderNotFoundException {
		// read the content of the item Resource
		String content = Helpers.readFile(item.getFile().getPath());

		// build the providers
		schema = schemaService.getSchemaForContent(content);
		IdentifierProvider identifierProvider = (IdentifierProvider) MetadataProviderFactory.create(schema,
				Metadata.Identifier);
		VisibilityProvider visibilityProvider = (VisibilityProvider) MetadataProviderFactory.create(schema,
				Metadata.Visibility);
		LandingPageProvider landingPageProvider = (LandingPageProvider) MetadataProviderFactory.create(schema,
				Metadata.LandingPage);

		// obtain the necessary information from the providers
		String identifierValue = identifierProvider.get(content);
		String landingPage = landingPageProvider.get(content);

		// add new Record, Identifier, URL and Version
		Record record = addRecord(visibilityProvider.get(content));
		addIdentifier(identifierValue, record);
		addURL(landingPage, record);
		addVersion(content, record);

		// append identifierValue to the outputFile path for use in next step
		Helpers.appendToFile(outputFilePath, identifierValue);
		return null;
	}

	private Record addRecord(boolean visible) {
		// create the record

		Record record = new Record();
		record.setCreatedAt(new Date());
		record.setOwnerID(UUID.fromString(creatorID));
		record.setOwnerType(Record.OwnerType.valueOf(ownerType));
		record.setVisible(visible);
		record.setAllocationID(UUID.fromString(allocationID));
		record.setCreatorID(UUID.fromString(creatorID));
		return recordService.save(record);
	}

	private void addIdentifier(String identifierValue, Record record) {
		// create the identifier
		Identifier identifier = new Identifier();
		identifier.setCreatedAt(new Date());
		identifier.setRecord(record);
		identifier.setType(Identifier.Type.IGSN);
		identifier.setValue(identifierValue);
		identifier.setStatus(Identifier.Status.PENDING);
		identifierService.save(identifier);
	}

	private void addURL(String urlValue, Record record) {
		URL url = new URL();
		url.setCreatedAt(new Date());
		url.setRecord(record);
		url.setUrl(urlValue);
		urlService.create(url);
	}

	private void addVersion(String content, Record record) {
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
