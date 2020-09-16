package au.edu.ardc.registry.igsn.job.config;

import au.edu.ardc.registry.common.repository.IdentifierRepository;
import au.edu.ardc.registry.common.repository.RecordRepository;
import au.edu.ardc.registry.common.repository.URLRepository;
import au.edu.ardc.registry.common.repository.VersionRepository;
import au.edu.ardc.registry.common.service.*;
import au.edu.ardc.registry.igsn.job.reader.IGSNItemReader;
import au.edu.ardc.registry.igsn.job.reader.PayloadContentReader;
import au.edu.ardc.registry.igsn.job.tasklet.PayloadChunkerTasklet;
import au.edu.ardc.registry.igsn.service.IGSNService;
import au.edu.ardc.registry.job.listener.JobCompletionListener;
import au.edu.ardc.registry.job.processor.IngestProcessor;
import au.edu.ardc.registry.igsn.job.processor.MintIGSNProcessor;
import au.edu.ardc.registry.job.writer.NoOpItemWriter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
public class IGSNMintJobConfig {

	@Autowired
	public JobBuilderFactory jobBuilderFactory;

	@Autowired
	public StepBuilderFactory stepBuilderFactory;

	@Autowired
	SchemaService schemaService;

	@Autowired
	KeycloakService kcService;

	@Autowired
	IGSNService igsnService;

	@Autowired
	IdentifierRepository identifierRepository;

	@Autowired
	RecordService recordService;

	@Autowired
	ValidationService validationService;

	@Autowired
	RecordRepository recordRepository;

	@Autowired
	VersionService versionService;

	@Autowired
	IdentifierService identifierService;

	@Autowired
	VersionRepository versionRepository;

	@Autowired
	URLRepository urlRepository;

	@Bean
	public Job IGSNImportJob() {
		return jobBuilderFactory.get("IGSNImportJob").incrementer(new RunIdIncrementer())
				.listener(new JobCompletionListener()).flow(chunk()).next(ingest()).next(registration()).end().build();
	}

	@Bean
	public Step chunk() {
		return stepBuilderFactory.get("chunk").tasklet(payloadChunkerTasklet()).build();
	}

	@Bean
	public PayloadChunkerTasklet payloadChunkerTasklet() {
		return new PayloadChunkerTasklet().setSchemaService(schemaService);
	}

	@Bean
	public Step ingest() {
		return stepBuilderFactory.get("ingest").<String, Resource>chunk(1).reader(new PayloadContentReader())
				.processor(new IngestProcessor(schemaService, validationService, identifierRepository, recordRepository,
						versionRepository, urlRepository))
				.writer(new NoOpItemWriter<>()).build();
	}

	@Bean
	public Step registration() {
		return stepBuilderFactory.get("registration").<String, String>chunk(1).reader(new IGSNItemReader())
				.processor(new MintIGSNProcessor(schemaService, kcService, identifierRepository, recordService,
						versionService, versionRepository))
				.writer(new NoOpItemWriter<>()).build();
	}

}
