package au.edu.ardc.registry.igsn.job.reader;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@StepScope
// public class XMLFragmentReader extends StaxEventItemReader<String> {
public class XMLFragmentReader {

	private String filePath;

	private String rootElementName;

	public XMLFragmentReader() {

		super();
	}

	@BeforeStep
	public void beforeStep(StepExecution stepExecution) {
		JobParameters jobParameters = stepExecution.getJobParameters();
		this.rootElementName = jobParameters.getString("rootElement");
		this.filePath = jobParameters.getString("filePath");
		// init();
	}

	// void init() {
	// this.setName("XMLFragmentReader");
	// this.setResource(new FileSystemResource(new File(filePath)));
	// this.setFragmentRootElementName(this.rootElementName);
	// }

}
