package au.edu.ardc.registry.igsn.job.config;

import au.edu.ardc.registry.igsn.job.listener.IGSNJobListener;
import au.edu.ardc.registry.igsn.job.processor.ReserveIGSNProcessor;
import au.edu.ardc.registry.igsn.job.reader.IGSNItemReader;
import au.edu.ardc.registry.igsn.job.writer.IGSNItemWriter;
import au.edu.ardc.registry.common.repository.IdentifierRepository;
import au.edu.ardc.registry.common.repository.RecordRepository;
import au.edu.ardc.registry.igsn.service.IGSNService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DeadlockLoserDataAccessException;

@Configuration
public class IGSNReserveJobConfig {

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Autowired
    RecordRepository recordRepository;

    @Autowired
    IdentifierRepository identifierRepository;

    @Autowired
    IGSNService igsnService;

    @Autowired
    TaskExecutor asyncTaskExecutor;

    @Bean(name = "ReserveIGSNJob")
    public Job ReserveIGSNJob() {
        return jobBuilderFactory.get("ReserveIGSNJob")
                .listener(new IGSNJobListener(igsnService))
                .flow(reserveIGSN())
                .end().build();
    }

    @Bean
    public Step reserveIGSN() {
        return stepBuilderFactory.get("Reserve IGSN")
                .<String, String>chunk(1)
                .reader(new IGSNItemReader())
                .processor(new ReserveIGSNProcessor(recordRepository, identifierRepository, igsnService))
                .writer(new IGSNItemWriter())
                .faultTolerant()
                .retryLimit(3)
                .retry(DeadlockLoserDataAccessException.class)
                .build();
    }
}
