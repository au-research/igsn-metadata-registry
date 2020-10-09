package au.edu.ardc.registry.igsn.job.reader;

import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.model.Attribute;
import au.edu.ardc.registry.igsn.service.IGSNRequestService;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

import static java.nio.file.Files.newDirectoryStream;
import static java.util.Spliterators.spliteratorUnknownSize;

@Component
@StepScope
public class PayloadContentReader extends MultiResourceItemReader<Resource> {

	private String chunkContentsDir;

	final IGSNRequestService igsnRequestService;

	public PayloadContentReader(IGSNRequestService igsnRequestService) {
		super();
		this.igsnRequestService = igsnRequestService;
	}

	@BeforeStep
	public void beforeStep(StepExecution stepExecution) throws IOException {
		JobParameters jobParameters = stepExecution.getJobParameters();
		String requestID = jobParameters.getString("IGSNServiceRequestID");
		Request request = igsnRequestService.findById(requestID);
		chunkContentsDir = request.getAttribute(Attribute.CHUNKED_PAYLOAD_PATH);
		init();
	}

	void init() throws IOException {
		setName("PayloadContentReader");
		ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
		Resource[] resources = resourcePatternResolver.getResources("file:" + chunkContentsDir + File.separator + "*");
		setResources(resources);
		setDelegate(new ResourcePassthroughReader());
	}

}
