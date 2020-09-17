package au.edu.ardc.registry.job.processor;

import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.URL;
import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.common.model.Allocation;
import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.provider.IdentifierProvider;
import au.edu.ardc.registry.common.provider.LandingPageProvider;
import au.edu.ardc.registry.common.provider.Metadata;
import au.edu.ardc.registry.common.provider.MetadataProviderFactory;
import au.edu.ardc.registry.common.repository.IdentifierRepository;
import au.edu.ardc.registry.common.repository.RecordRepository;
import au.edu.ardc.registry.common.repository.URLRepository;
import au.edu.ardc.registry.common.repository.VersionRepository;
import au.edu.ardc.registry.common.service.*;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.igsn.entity.IGSNServiceRequest;
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

public class UpdateProcessor implements ItemProcessor<Resource, Resource> {

	private IdentifierRepository identifierRepository;

	private RecordRepository recordRepository;

	private VersionRepository versionRepository;

	private URLRepository urlRepository;

	private SchemaService schemaService;

	private String creatorID;

	private String outputFilePath;

	private String allocationID;

	private String ownerType;

	private Schema schema;

	public UpdateProcessor(SchemaService schemaService, ValidationService validationService,
			IdentifierRepository identifierRepository, RecordRepository recordRepository,
			VersionRepository versionRepository, URLRepository urlRepository) {

		this.identifierRepository = identifierRepository;
		this.recordRepository = recordRepository;
		this.versionRepository = versionRepository;
		this.urlRepository = urlRepository;
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

	private void processContent(Resource item) throws IOException {
		System.out.println("File is LOADING:" + item.getFilename());
		String content = Helpers.readFile(item.getFile().getPath());
		schema = schemaService.getSchemaForContent(content);
		IdentifierProvider identifierProvider = (IdentifierProvider) MetadataProviderFactory.create(schema,
				Metadata.Identifier);
		assert identifierProvider != null;
		String identifieValue = identifierProvider.get(content);
		LandingPageProvider landingPageProvider = (LandingPageProvider) MetadataProviderFactory.create(schema,
				Metadata.LandingPage);
		assert landingPageProvider != null;
		String landingPage = landingPageProvider.get(content);
		// TODO find corresponding record and update it!

		//
		//
		// Record record = addRecord();
		// System.out.println("record id : " + record.getId());
		// addURL(landingPage, record);
		// addVersion(content, record);
		Helpers.appendToFile(outputFilePath, identifieValue);
	}

	private Record updateRecord() {
		// create the record
		Record record = new Record();
		record.setCreatedAt(new Date());
		record.setOwnerID(UUID.fromString(creatorID));
		record.setOwnerType(Record.OwnerType.valueOf(ownerType));
		record.setVisible(true);
		record.setAllocationID(UUID.fromString(allocationID));
		record.setCreatorID(UUID.fromString(creatorID));
		System.out.println("addRecord");
		return recordRepository.saveAndFlush(record);
	}

	private void updateURL(String urlValue, Record record) {
		URL url = new URL();
		url.setCreatedAt(new Date());
		url.setRecord(record);
		url.setUrl(urlValue);
		System.out.println("addURL:" + url.getId());

		urlRepository.saveAndFlush(url);

	}

	private void updateVersion(String content, Record record) {
		Version version = new Version();
		version.setRecord(record);
		version.setSchema(schema.getId());
		version.setContent(content.getBytes());
		version.setCreatorID(UUID.fromString(creatorID));
		version.setCreatedAt(new Date());
		version.setCurrent(true);
		version.setHash(VersionService.getHash(content));
		System.out.println("addVersion:" + version.getId());
		versionRepository.saveAndFlush(version);
	}

}
