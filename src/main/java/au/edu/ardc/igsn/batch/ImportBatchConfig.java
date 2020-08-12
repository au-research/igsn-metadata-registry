package au.edu.ardc.igsn.batch;

import au.edu.ardc.igsn.batch.listener.JobCompletionListener;
import au.edu.ardc.igsn.batch.processor.IngestProcessor;
import au.edu.ardc.igsn.batch.processor.RegistrationProcessor;
import au.edu.ardc.igsn.batch.processor.ValidatePayloadProcessor;
import au.edu.ardc.igsn.batch.writer.NoOpItemWriter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.listener.JobParameterExecutionContextCopyListener;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ImportBatchConfig {

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job IGSNImportJob() {
        return jobBuilderFactory.get("IGSNImportJob")
                .incrementer(new RunIdIncrementer())
                .listener(new JobCompletionListener())
                .flow(validate())
                .next(ingest())
                .next(registration())
                .end().build();
    }

    @Bean
    public Step validate() {
        return stepBuilderFactory.get("validate")
                .<String, String>chunk(1)
                .reader(new FlatFileItemReader<>())
                .processor(new ValidatePayloadProcessor())
                .writer(new NoOpItemWriter<>())
                .build();
    }

    @Bean
    public Step ingest() {
        return stepBuilderFactory.get("ingest")
                .<String, String>chunk(1)
                .reader(new FlatFileItemReader<>())
                .processor(new IngestProcessor())
                .writer(new NoOpItemWriter<>())
                .build();
    }

    @Bean
    public Step registration() {
        return stepBuilderFactory.get("registration")
                .<String, String>chunk(1)
                .reader(new FlatFileItemReader<>())
                .processor(new RegistrationProcessor())
                .writer(new NoOpItemWriter<>())
                .build();
    }

}
