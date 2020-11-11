//package au.edu.ardc.registry.igsn.config;
//
//import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
//import org.springframework.batch.core.launch.JobLauncher;
//import org.springframework.batch.core.launch.support.SimpleJobLauncher;
//import org.springframework.batch.core.repository.JobRepository;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
//
//@Configuration
//@EnableBatchProcessing
//public class IGSNBatchConfig {
//
//	private final JobRepository jobRepository;
//
//	public IGSNBatchConfig(JobRepository jobRepository) {
//		this.jobRepository = jobRepository;
//	}
//
//	/**
//	 * The single queue used for IGSN Jobs. Applicable for IGSN operation only to prevent
//	 * undesirable circumstances when IGSN are interacted with whilst under processing
//	 * @return a {@link JobLauncher} with a single queue {@link ThreadPoolTaskExecutor}
//	 * @throws Exception when launcher failed to set properties for some reason
//	 */
//	@Bean(name = "IGSNSingleQueueJobLauncher")
//	public JobLauncher igsnQueueJobLauncher() throws Exception {
//		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
//		taskExecutor.setCorePoolSize(1);
//		taskExecutor.setMaxPoolSize(1);
//		taskExecutor.afterPropertiesSet();
//
//		SimpleJobLauncher launcher = new SimpleJobLauncher();
//		launcher.setJobRepository(jobRepository);
//		launcher.setTaskExecutor(taskExecutor);
//		launcher.afterPropertiesSet();
//		return launcher;
//	}
//
//}
