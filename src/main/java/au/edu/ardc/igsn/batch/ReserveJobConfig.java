package au.edu.ardc.igsn.batch;

import au.edu.ardc.igsn.batch.processor.ReserveIGSNProcessor;
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

import java.io.File;

@Configuration
public class ReserveJobConfig {

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Bean(name = "ReserveIGSNJob")
    public Job ReserveIGSNJob() {
        return jobBuilderFactory.get("ReserveIGSNJob")
                .flow(reserveIGSN())
                .end().build();
    }

    @Bean
    public Step reserveIGSN() {
        return stepBuilderFactory.get("Reserve IGSN")
                .<String, String>chunk(1)
                .reader(IGSNItemReader(null))
                .processor(new ReserveIGSNProcessor())
                .writer(IGSNItemWriter(null))
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemWriter<String> IGSNItemWriter(@Value("#{jobParameters[targetPath]}") String targetPath) {
        return new FlatFileItemWriterBuilder<String>()
                .name("IGSNItemWriter")
                .resource(new FileSystemResource(new File(targetPath)))
                .lineAggregator(new PassThroughLineAggregator<>())
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<String> IGSNItemReader(@Value("#{jobParameters[filePath]}") String filePath) {
        return new FlatFileItemReaderBuilder<String>()
                .name("IGSNItemReader")
                .resource(new FileSystemResource(new File(filePath)))
                .lineMapper(new PassThroughLineMapper())
                .build();
    }
}
