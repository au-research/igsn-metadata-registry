package au.edu.ardc.registry.igsn.job.writer;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.PassThroughLineAggregator;
import org.springframework.core.io.FileSystemResource;

import java.io.File;

public class IGSNItemWriter extends FlatFileItemWriter<String> {

    private String targetPath;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        JobParameters jobParameters = stepExecution.getJobParameters();
        this.targetPath = jobParameters.getString("targetPath");
        init();
    }

    void init() {
        this.setName("IGSNItemWriter");
        this.setResource(new FileSystemResource(new File(targetPath)));
        this.setLineAggregator(new PassThroughLineAggregator<>());
    }
}
