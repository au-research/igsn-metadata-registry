package au.edu.ardc.registry.job.processor;

import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.URL;
import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.common.model.Allocation;
import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.provider.*;
import au.edu.ardc.registry.common.repository.IdentifierRepository;
import au.edu.ardc.registry.common.repository.RecordRepository;
import au.edu.ardc.registry.common.repository.URLRepository;
import au.edu.ardc.registry.common.repository.VersionRepository;
import au.edu.ardc.registry.common.service.*;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.exception.ContentProviderNotFoundException;
import au.edu.ardc.registry.igsn.entity.IGSNServiceRequest;
import au.edu.ardc.registry.igsn.service.IGSNVersionService;
import au.edu.ardc.registry.igsn.validator.UserAccessValidator;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

public class IngestRecordProcessor implements ItemProcessor<Resource, Resource> {

	private SchemaService schemaService;

	private KeycloakService kcService;

	private IdentifierService identifierService;

	private RecordService recordService;

	private IGSNVersionService igsnVersionService;

	private URLService urlService;

	private String creatorID;

	private String outputFilePath;

	private String allocationID;

	private String ownerType;

	private Schema schema;

	public IngestRecordProcessor(SchemaService schemaService, IdentifierService identifierService,
			RecordService recordService, IGSNVersionService versionService, URLService urlService) {

		this.identifierService = identifierService;
		this.recordService = recordService;
		this.igsnVersionService = versionService;
		this.urlService = urlService;
		this.schemaService = schemaService;
	}

	@BeforeStep
	public void beforeStep(final StepExecution stepExecution) {
		JobParameters jobParameters = stepExecution.getJobParameters();
		this.creatorID = jobParameters.getString("creatorID");
		this.outputFilePath = jobParameters.getString("filePath");
		this.allocationID = jobParameters.getString("allocationID");
		this.ownerType = jobParameters.getString("ownerType");
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
		VisibilityProvider visibilityProvider = (VisibilityProvider) MetadataProviderFactory.create(schema,
				Metadata.Visibility);
		String isPublic = visibilityProvider.get(content);
		boolean visible = false;
		if (isPublic.toLowerCase().equals("true"))
			visible = true;
		Record record = addRecord(visible);
		addIdentifier(identifierValue, record);
		addURL(landingPage, record);
		addVersion(content, record);
		Helpers.appendToFile(outputFilePath, identifierValue);
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
		return recordService.create(record);
	}

	private void addIdentifier(String identifierValue, Record record) {
		// create the identifier
		Identifier identifier = new Identifier();
		identifier.setCreatedAt(new Date());
		identifier.setRecord(record);
		identifier.setType(Identifier.Type.IGSN);
		identifier.setValue(identifierValue);
		identifier.setStatus(Identifier.Status.PENDING);
		identifierService.create(identifier);

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