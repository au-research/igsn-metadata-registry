package au.edu.ardc.registry.igsn.job.reader;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.StreamSupport;
import static java.util.Spliterator.ORDERED;
import static java.nio.file.Files.newDirectoryStream;
import static java.util.Spliterators.spliteratorUnknownSize;

@Component
@StepScope
public class PayloadContentReader extends MultiResourceItemReader {

	private String chunkContentsDir;

	public PayloadContentReader() {
		super();
	}

	@BeforeStep
	public void beforeStep(StepExecution stepExecution) throws IOException {
		JobParameters jobParameters = stepExecution.getJobParameters();
		chunkContentsDir = jobParameters.getString("chunkContentsDir");
		System.out.println("Processing Directory: " + chunkContentsDir);
		init();
	}

	void init() throws IOException {
		this.setName("PayloadContentReader");
		ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
		Resource[] resources = resourcePatternResolver.getResources("file:" + chunkContentsDir + File.separator + "*");
		System.out.println("Number of Files: " + resources.length);
		super.setResources(resources);
		super.setDelegate(new ResourcePassthroughReader());
	}

}
