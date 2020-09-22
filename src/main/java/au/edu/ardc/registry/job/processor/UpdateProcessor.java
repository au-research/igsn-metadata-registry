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
import au.edu.ardc.registry.igsn.service.IGSNVersionService;
import au.edu.ardc.registry.igsn.validator.UserAccessValidator;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class UpdateProcessor implements ItemProcessor<Resource, Resource> {

	private IdentifierService identifierService;

	private RecordService recordService;

	private IGSNVersionService igsnVersionService;

	private URLService urlService;

	private SchemaService schemaService;

	private String creatorID;

	private String outputFilePath;

	private String allocationID;

	private String ownerType;

	private Schema schema;

	public UpdateProcessor(SchemaService schemaService, IdentifierService identifierService,
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

	private void processContent(Resource item) throws IOException {
		System.out.println("File is LOADING:" + item.getFilename());
		String content = Helpers.readFile(item.getFile().getPath());
		schema = schemaService.getSchemaForContent(content);
		IdentifierProvider identifierProvider = (IdentifierProvider) MetadataProviderFactory.create(schema,
				Metadata.Identifier);
		assert identifierProvider != null;
		String identifierValue = identifierProvider.get(content);
		LandingPageProvider landingPageProvider = (LandingPageProvider) MetadataProviderFactory.create(schema,
				Metadata.LandingPage);
		assert landingPageProvider != null;
		String landingPage = landingPageProvider.get(content);
		Identifier existingIdentifier = identifierService.findByValueAndType(identifierValue, Identifier.Type.IGSN);
		Record record = existingIdentifier.getRecord();
		record.setModifierID(UUID.fromString(creatorID));
		record.setModifiedAt(new Date());
		List<Version> cVersions = record.getCurrentVersions();

		for (Version v : cVersions) {
			if (v.getSchema().equals(schema.getId())) {
				igsnVersionService.end(v, UUID.fromString(creatorID));
			}
		}
		addNewVersion(content, record);
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
		System.out.println("addNewVersion:" + version.getId());
		igsnVersionService.save(version);
	}

}
