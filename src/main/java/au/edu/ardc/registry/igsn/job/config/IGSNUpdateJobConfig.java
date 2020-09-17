package au.edu.ardc.registry.igsn.job.config;

import au.edu.ardc.registry.common.repository.IdentifierRepository;
import au.edu.ardc.registry.common.repository.RecordRepository;
import au.edu.ardc.registry.common.repository.URLRepository;
import au.edu.ardc.registry.common.repository.VersionRepository;
import au.edu.ardc.registry.common.service.*;
import au.edu.ardc.registry.igsn.job.processor.UpdateIGSNProcessor;
import au.edu.ardc.registry.igsn.job.reader.IGSNItemReader;
import au.edu.ardc.registry.igsn.job.reader.PayloadContentReader;
import au.edu.ardc.registry.igsn.job.tasklet.PayloadChunkerTasklet;
import au.edu.ardc.registry.igsn.service.IGSNService;
import au.edu.ardc.registry.job.listener.JobCompletionListener;
import au.edu.ardc.registry.job.processor.UpdateProcessor;
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
public class IGSNUpdateJobConfig {

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
	public Job IGSNUpdateJob() {
		return jobBuilderFactory.get("IGSNUpdatetJob").incrementer(new RunIdIncrementer())
				.listener(new JobCompletionListener()).flow(chunkUpdate()).next(update()).next(registrationUpdate())
				.end().build();
	}

	@Bean
	public Step chunkUpdate() {
		return stepBuilderFactory.get("chunk-update").tasklet(payloadChunkerTasklet2()).build();
	}

	@Bean
	public PayloadChunkerTasklet payloadChunkerTasklet2() {
		return new PayloadChunkerTasklet().setSchemaService(schemaService);
	}

	@Bean
	public Step update() {
		return stepBuilderFactory.get("update").<String, Resource>chunk(1).reader(new PayloadContentReader())
				.processor(new UpdateProcessor(schemaService, validationService, identifierRepository, recordRepository,
						versionRepository, urlRepository))
				.writer(new NoOpItemWriter<>()).build();
	}

	@Bean
	public Step registrationUpdate() {
		return stepBuilderFactory.get("registration-update").<String, String>chunk(1).reader(new IGSNItemReader())
				.processor(new UpdateIGSNProcessor(schemaService, kcService, identifierRepository, recordService,
						versionService, versionRepository))
				.writer(new NoOpItemWriter<>()).build();
	}

}
