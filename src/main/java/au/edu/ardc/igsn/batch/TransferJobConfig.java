package au.edu.ardc.igsn.batch;

import au.edu.ardc.igsn.batch.listener.IGSNJobListener;
import au.edu.ardc.igsn.batch.processor.TransferIGSNProcessor;
import au.edu.ardc.igsn.batch.reader.IGSNItemReader;
import au.edu.ardc.igsn.batch.writer.IGSNItemWriter;
import au.edu.ardc.igsn.repository.IdentifierRepository;
import au.edu.ardc.igsn.repository.RecordRepository;
import au.edu.ardc.igsn.service.IGSNService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DeadlockLoserDataAccessException;

@Configuration
public class TransferJobConfig {

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Autowired
    IdentifierRepository identifierRepository;

    @Autowired
    RecordRepository recordRepository;

    @Autowired
    IGSNService igsnService;

    @Bean(name="TransferIGSNJob")
    public Job TransferIGSNJob() {
        return jobBuilderFactory.get("TransferIGSNJob")
                .listener(new IGSNJobListener(igsnService))
                .flow(transferIGSN())
                .end().build();
    }

    @Bean
    public Step transferIGSN() {
        return stepBuilderFactory.get("Transfer IGSN")
                .<String, String>chunk(1)
                .reader(new IGSNItemReader())
                .processor(new TransferIGSNProcessor(recordRepository, identifierRepository, igsnService))
                .writer(new IGSNItemWriter())
                .faultTolerant().retryLimit(3)
                .retry(DeadlockLoserDataAccessException.class)
                .build();
    }

}
