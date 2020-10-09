package au.edu.ardc.registry.igsn.job.config;

import au.edu.ardc.registry.common.service.*;
import au.edu.ardc.registry.igsn.job.processor.MintIGSNProcessor;
import au.edu.ardc.registry.igsn.job.reader.IGSNItemReader;
import au.edu.ardc.registry.igsn.job.reader.PayloadContentReader;
import au.edu.ardc.registry.igsn.job.tasklet.PayloadChunkerTasklet;
import au.edu.ardc.registry.igsn.service.IGSNRequestService;
import au.edu.ardc.registry.igsn.service.IGSNVersionService;
import au.edu.ardc.registry.job.listener.JobCompletionListener;
import au.edu.ardc.registry.job.processor.IngestRecordProcessor;
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
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.web.client.HttpServerErrorException;

@Configuration
public class IGSNMintJobConfig {

	@Autowired
	public JobBuilderFactory jobBuilderFactory;

	@Autowired
	public StepBuilderFactory stepBuilderFactory;
	@Autowired
	IGSNRequestService igsnRequestService;
	@Autowired
	BackOffPolicy backOffPolicy;
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

	@Bean
	public Job IGSNImportJob() {
		// @formatter:off
		return jobBuilderFactory.get("IGSNImportJob").incrementer(new RunIdIncrementer())
				.listener(new JobCompletionListener())
				.flow(chunkPayload())
				.next(ingest())
				.next(registration())
				.end()
				.build();
		// @formatter:on
	}

	@Bean
	public Step chunkPayload() {
		return stepBuilderFactory.get("chunkPayload")
				.tasklet(new PayloadChunkerTasklet(schemaService, igsnRequestService)).build();
	}

	@Bean
	public Step ingest() {
		//@formatter:off
		return stepBuilderFactory.get("ingest")
				.<Resource, Resource>chunk(1)
				.reader(new PayloadContentReader(igsnRequestService))
				.processor(new IngestRecordProcessor(schemaService, identifierService, recordService,
						igsnVersionService, urlService, igsnRequestService))
				.writer(new NoOpItemWriter<>())
				.build();
		//@formatter:on
	}

	@Bean
	public Step registration() {
		//@formatter:off
		return stepBuilderFactory.get("registration")
				.<String, String>chunk(1)
				.reader(new IGSNItemReader(igsnRequestService))
				.processor(new MintIGSNProcessor(schemaService, kcService, identifierService,
						igsnVersionService, igsnRequestService))
				.faultTolerant()
				.retryLimit(5)
				.retry(HttpServerErrorException.class)
				.backOffPolicy(backOffPolicy)
				.writer(new NoOpItemWriter<>()).build();
		//@formatter:on
	}

}
