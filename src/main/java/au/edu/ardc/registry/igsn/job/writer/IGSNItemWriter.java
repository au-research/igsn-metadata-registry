package au.edu.ardc.registry.igsn.job.writer;

import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.model.Attribute;
import au.edu.ardc.registry.igsn.service.IGSNRequestService;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.PassThroughLineAggregator;
import org.springframework.core.io.FileSystemResource;

import java.io.File;

public class IGSNItemWriter extends FlatFileItemWriter<String> {

	private String targetPath;

	private final IGSNRequestService igsnRequestService;

	public IGSNItemWriter(IGSNRequestService igsnRequestService) {
		this.igsnRequestService = igsnRequestService;
	}

	@BeforeStep
	public void beforeStep(StepExecution stepExecution) {
		JobParameters jobParameters = stepExecution.getJobParameters();
		String IGSNServiceRequestID = jobParameters.getString("IGSNServiceRequestID");
		Request request = igsnRequestService.findById(IGSNServiceRequestID);
		this.targetPath = request.getAttribute(Attribute.IMPORTED_IDENTIFIERS_PATH);
		init();
	}

	void init() {
		this.setName("IGSNItemWriter");
		this.setResource(new FileSystemResource(new File(targetPath)));
		this.setLineAggregator(new PassThroughLineAggregator<>());
	}

}
