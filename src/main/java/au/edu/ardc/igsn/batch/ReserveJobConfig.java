package au.edu.ardc.igsn.batch;

import au.edu.ardc.igsn.batch.listener.IGSNJobListener;
import au.edu.ardc.igsn.batch.processor.ReserveIGSNProcessor;
import au.edu.ardc.igsn.batch.reader.IGSNItemReader;
import au.edu.ardc.igsn.batch.writer.IGSNItemWriter;
import au.edu.ardc.igsn.repository.IdentifierRepository;
import au.edu.ardc.igsn.repository.RecordRepository;
import au.edu.ardc.igsn.service.IGSNService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.mapping.PassThroughLineMapper;
import org.springframework.batch.item.file.transform.PassThroughLineAggregator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DeadlockLoserDataAccessException;

import java.io.File;

@Configuration
public class ReserveJobConfig {

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
