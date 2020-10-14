package au.edu.ardc.registry.igsn.job.config;

import au.edu.ardc.registry.common.service.RequestService;
import au.edu.ardc.registry.igsn.job.listener.IGSNJobListener;
import au.edu.ardc.registry.igsn.job.processor.TransferIGSNProcessor;
import au.edu.ardc.registry.igsn.job.reader.IGSNItemReader;
import au.edu.ardc.registry.igsn.job.writer.IGSNItemWriter;
import au.edu.ardc.registry.common.repository.IdentifierRepository;
import au.edu.ardc.registry.common.repository.RecordRepository;
import au.edu.ardc.registry.igsn.service.IGSNRequestService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DeadlockLoserDataAccessException;

@Configuration
public class IGSNTransferJobConfig {

	@Autowired
	public JobBuilderFactory jobBuilderFactory;

	@Autowired
	public StepBuilderFactory stepBuilderFactory;

	@Autowired
	IdentifierRepository identifierRepository;

	@Autowired
	RecordRepository recordRepository;

	@Autowired
	IGSNRequestService igsnRequestService;

	@Autowired
	RequestService requestService;

	@Bean(name = "TransferIGSNJob")
	public Job TransferIGSNJob() {
		return jobBuilderFactory.get("TransferIGSNJob")
				.listener(new IGSNJobListener(igsnRequestService)).flow(transferIGSN()).end().build();
	}

	@Bean
	public Step transferIGSN() {
		return stepBuilderFactory.get("Transfer IGSN").<String, String>chunk(1)
				.reader(new IGSNItemReader(igsnRequestService))
				.processor(new TransferIGSNProcessor(recordRepository, identifierRepository, igsnRequestService,
						requestService))
				.writer(new IGSNItemWriter(igsnRequestService)).faultTolerant().retryLimit(3)
				.retry(DeadlockLoserDataAccessException.class).build();
	}

}
