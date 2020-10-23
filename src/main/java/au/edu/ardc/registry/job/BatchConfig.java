package au.edu.ardc.registry.job;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sql.DataSource;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

	@Autowired
	public JobRepository jobRepository;

	@Bean(name = "standardJobLauncher")
	public JobLauncher standardJobLauncher() throws Exception {
		SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
		jobLauncher.setJobRepository(jobRepository);
		jobLauncher.afterPropertiesSet();
		return jobLauncher;
	}

	@Bean(name = "asyncJobLauncher")
	public JobLauncher asyncJobLauncher() throws Exception {
		SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
		jobLauncher.setJobRepository(jobRepository);
		jobLauncher.setTaskExecutor(new SimpleAsyncTaskExecutor());
		jobLauncher.afterPropertiesSet();
		return jobLauncher;
	}

	@Bean(name = "asyncTaskExecutor")
	public TaskExecutor asyncTaskExecutor() {
		SimpleAsyncTaskExecutor asyncTaskExecutor = new SimpleAsyncTaskExecutor("IGSNRegistryAsyncTaskExecutor");
		asyncTaskExecutor.setConcurrencyLimit(5);
		return asyncTaskExecutor;
	}

	@Bean
	public BackOffPolicy backOffPolicy() {
		ExponentialBackOffPolicy exponentialBackOffPolicy = new ExponentialBackOffPolicy();
		exponentialBackOffPolicy.setInitialInterval(1000);
		exponentialBackOffPolicy.setMultiplier(2.0);
		exponentialBackOffPolicy.setMaxInterval(10000);
		return exponentialBackOffPolicy;
	}

	@Autowired
	public void setDataSource(DataSource dataSource) {
	}

}
