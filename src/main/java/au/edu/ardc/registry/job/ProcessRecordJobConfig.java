package au.edu.ardc.registry.job;

import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.job.processor.RecordTitleProcessor;
import au.edu.ardc.registry.job.reader.RecordReader;
import au.edu.ardc.registry.job.writer.NoOpItemWriter;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.repository.RecordRepository;
import au.edu.ardc.registry.common.service.RecordService;
import au.edu.ardc.registry.common.service.VersionService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DeadlockLoserDataAccessException;

@Configuration
public class ProcessRecordJobConfig {

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

	@Autowired
	SchemaService schemaService;

	@Bean(name = "ProcessRecordJob")
	public Job ProcessRecordJob() {
		return jobBuilderFactory.get("ProcessRecordJob").flow(processTitles()).end().build();
	}

	@Bean
	public Step processTitles() {
		return stepBuilderFactory.get("Process Titles").<Record, Record>chunk(10)
				.reader(new RecordReader(recordRepository))
				.processor(new RecordTitleProcessor(versionService, recordService, schemaService))
				.writer(new NoOpItemWriter<>()).faultTolerant().retryLimit(3)
				.retry(DeadlockLoserDataAccessException.class)
				// .taskExecutor(concurrentTaskExecutor())
				.build();
	}

	@Bean
	public TaskExecutor concurrentTaskExecutor() {
		SimpleAsyncTaskExecutor asyncTaskExecutor = new SimpleAsyncTaskExecutor("igsnRegistryParallelThreads");
		asyncTaskExecutor.setConcurrencyLimit(5);
		return asyncTaskExecutor;
	}

}
