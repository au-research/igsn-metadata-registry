package au.edu.ardc.igsn.batch;

import au.edu.ardc.igsn.TestHelper;
import au.edu.ardc.igsn.entity.Identifier;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.repository.IdentifierRepository;
import au.edu.ardc.igsn.repository.RecordRepository;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBatchTest
@ExtendWith(SpringExtension.class)
@EnableAutoConfiguration
@ContextConfiguration(classes = {
        BatchConfig.class, ReserveJobConfig.class
})
class ReserveJobConfigTest {

    @MockBean
    IdentifierRepository identifierRepository;

    @MockBean
    RecordRepository recordRepository;

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Test
    void job_validParams_createdTargetFile() throws Exception {
        String targetPath = "/tmp/" + UUID.randomUUID().toString() + ".txt";

        JobParametersBuilder params = new JobParametersBuilder();
        params.addString("filePath", "src/test/resources/data/igsn.txt");
        params.addString("targetPath", targetPath);

        when(recordRepository.saveAndFlush(any(Record.class))).thenReturn(TestHelper.mockRecord(UUID.randomUUID()));
        when(identifierRepository.saveAndFlush(any(Identifier.class))).thenReturn(TestHelper.mockIdentifier(UUID.randomUUID()));

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(params.toJobParameters());
        assertThat(jobExecution.getExitStatus().getExitCode()).isEqualTo(BatchStatus.COMPLETED.toString());

        // 4 identifiers are created
        verify(identifierRepository, times(4)).saveAndFlush(any(Identifier.class));

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