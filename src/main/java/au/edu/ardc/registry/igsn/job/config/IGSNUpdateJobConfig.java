package au.edu.ardc.registry.igsn.job.config;

import au.edu.ardc.registry.common.service.*;
import au.edu.ardc.registry.igsn.job.listener.IGSNJobListener;
import au.edu.ardc.registry.igsn.job.processor.MintIGSNProcessor;
import au.edu.ardc.registry.igsn.job.processor.UpdateIGSNProcessor;
import au.edu.ardc.registry.igsn.job.reader.IGSNItemReader;
import au.edu.ardc.registry.igsn.job.reader.PayloadContentReader;
import au.edu.ardc.registry.igsn.job.tasklet.PayloadChunkerTasklet;
import au.edu.ardc.registry.igsn.service.IGSNRequestService;
import au.edu.ardc.registry.igsn.service.IGSNVersionService;
import au.edu.ardc.registry.job.listener.JobCompletionListener;
import au.edu.ardc.registry.job.processor.UpdateRecordProcessor;
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
	IGSNRequestService igsnRequestService;

	@Autowired
	RecordService recordService;

	@Autowired
	IGSNVersionService igsnVersionService;

	@Autowired
	IdentifierService identifierService;

	@Autowired
	URLService urlService;

	@Autowired
	BackOffPolicy backOffPolicy;

	@Bean
	public Job IGSNUpdateJob() {
		return jobBuilderFactory.get("IGSNUpdatetJob").incrementer(new RunIdIncrementer())
				.listener(new IGSNJobListener(igsnRequestService)).flow(chunkUpdatePayload()).next(update())
				.next(registrationUpdate()).end().build();
	}

	@Bean
	public Step chunkUpdatePayload() {
		return stepBuilderFactory.get("chunkUpdatePayload")
				.tasklet(new PayloadChunkerTasklet(schemaService, igsnRequestService)).build();
	}

	@Bean
	public Step update() {
		//@formatter:off
		return stepBuilderFactory.get("update").<Resource, Resource>chunk(1)
				.reader(new PayloadContentReader(igsnRequestService))
				.processor(new UpdateRecordProcessor(schemaService, identifierService, recordService,
						igsnVersionService, urlService, igsnRequestService))
				.writer(new NoOpItemWriter<>()).build();
		//@formatter:on
	}

	@Bean
	public Step registrationUpdate() {
		//@formatter:off
		return stepBuilderFactory.get("registration-update")
				.<String, String>chunk(1)
				.reader(new IGSNItemReader(igsnRequestService))
				.processor(new UpdateIGSNProcessor(schemaService, kcService, identifierService,
						igsnVersionService, igsnRequestService))
				.faultTolerant()
				.retryLimit(5)
				.retry(HttpServerErrorException.class)
				.backOffPolicy(backOffPolicy)
				.writer(new NoOpItemWriter<>()).build();
		//@formatter:on
	}

}
