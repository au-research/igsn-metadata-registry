package au.edu.ardc.registry.igsn.job.reader;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.PassThroughLineMapper;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@StepScope
public class IGSNItemReader extends FlatFileItemReader<String> {

    private String filePath;

    public IGSNItemReader() {
        super();
    }

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        JobParameters jobParameters = stepExecution.getJobParameters();
        this.filePath = jobParameters.getString("filePath");
        init();
    }

    void init() {
        this.setName("IGSNItemReader");
        this.setResource(new FileSystemResource(new File(filePath)));
        this.setLineMapper(new PassThroughLineMapper());
    }

}
