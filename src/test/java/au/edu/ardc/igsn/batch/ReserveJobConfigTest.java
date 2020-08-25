package au.edu.ardc.igsn.batch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest
@ExtendWith(SpringExtension.class)
@EnableAutoConfiguration
@ContextConfiguration(classes = {BatchConfig.class, ReserveJobConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ReserveJobConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Test
    void job_validParams_createdTargetFile() throws Exception {
        String targetPath = "/tmp/result.txt";

        JobParametersBuilder params = new JobParametersBuilder();
        params.addString("filePath", "src/test/resources/data/igsn.txt");
        params.addString("targetPath", targetPath);

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(params.toJobParameters());
        // JobInstance actual = jobExecution.getJobInstance();
        assertThat(jobExecution.getExitStatus().getExitCode()).isEqualTo(BatchStatus.COMPLETED.toString());
        assertThat(new File(targetPath).exists());
        new File(targetPath).deleteOnExit();
    }

    @Configuration
    @EnableBatchProcessing
    static class BatchTestConfig {
        @Bean
        JobLauncherTestUtils jobLauncherTestUtils() {
            return new JobLauncherTestUtils();
        }
    }
}