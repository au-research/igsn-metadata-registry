package au.edu.ardc.igsn.batch;

import au.edu.ardc.igsn.batch.listener.JobCompletionListener;
import au.edu.ardc.igsn.batch.processor.RecordTitleProcessor;
import au.edu.ardc.igsn.batch.reader.RecordReader;
import au.edu.ardc.igsn.batch.writer.NoOpItemWriter;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.repository.RecordRepository;
import au.edu.ardc.igsn.service.RecordService;
import au.edu.ardc.igsn.service.VersionService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.listener.JobParameterExecutionContextCopyListener;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;

import java.util.HashMap;

@Configuration
public class ProcessRecordBatchConfig {

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Autowired
    public RecordRepository recordRepository;

    @Autowired
    VersionService versionService;

    @Autowired
    RecordService recordService;

    @Bean
    public Job ProcessRecordJob() {
        return jobBuilderFactory.get("ProcessRecordJob")
                .flow(processTitles())
                .end().build();
    }

    @Bean
    public Step processTitles() {
        return stepBuilderFactory.get("Process Titles")
                .<Record, Record> chunk(10)
                .reader(new RecordReader(recordRepository))
                .processor(new RecordTitleProcessor(versionService, recordService))
                .writer(new NoOpItemWriter<>())
                .build();
    }
}
