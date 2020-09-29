package au.edu.ardc.registry.igsn.job.config;

import au.edu.ardc.registry.common.service.*;
import au.edu.ardc.registry.igsn.job.reader.IGSNItemReader;
import au.edu.ardc.registry.igsn.job.reader.PayloadContentReader;
import au.edu.ardc.registry.igsn.job.tasklet.PayloadChunkerTasklet;
import au.edu.ardc.registry.igsn.service.IGSNRequestService;
import au.edu.ardc.registry.igsn.service.IGSNVersionService;
import au.edu.ardc.registry.job.listener.JobCompletionListener;
import au.edu.ardc.registry.job.processor.IngestRecordProcessor;
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
	private SchemaService schemaService;

	@Autowired
	private KeycloakService kcService;

	@Autowired
	private IdentifierService identifierService;

	@Autowired
	private RecordService recordService;

	@Autowired
	private IGSNVersionService igsnVersionService;

	@Autowired
	private URLService urlService;

	@Autowired
	IGSNRequestService igsnRequestService;

	@Bean
	public Job IGSNImportJob() {
		return jobBuilderFactory.get("IGSNImportJob").incrementer(new RunIdIncrementer())
				.listener(new JobCompletionListener()).flow(chunkPayload()).next(ingest()).next(registration()).end()
				.build();
	}

	@Bean
	public Step chunkPayload() {
		return stepBuilderFactory.get("chunkPayload").tasklet(payloadChunkerTasklet()).build();
	}

	@Bean
	public PayloadChunkerTasklet payloadChunkerTasklet() {
		return new PayloadChunkerTasklet().setSchemaService(schemaService);
	}

	@Bean
	public Step ingest() {
		return stepBuilderFactory.get("ingest").<String, Resource>chunk(1).reader(new PayloadContentReader())
				.processor(new IngestRecordProcessor(schemaService, identifierService, recordService,
						igsnVersionService, urlService))
				.writer(new NoOpItemWriter<>()).build();
	}

	@Bean
	public Step registration() {
		return stepBuilderFactory.get("registration").<String, String>chunk(1).reader(new IGSNItemReader(igsnRequestService))
				.processor(new MintIGSNProcessor(schemaService, kcService, identifierService, recordService,
						igsnVersionService, urlService))
				.writer(new NoOpItemWriter<>()).build();
	}

}
