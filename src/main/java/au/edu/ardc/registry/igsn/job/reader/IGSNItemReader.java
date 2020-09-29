package au.edu.ardc.registry.igsn.job.reader;

import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.model.Attribute;
import au.edu.ardc.registry.igsn.service.IGSNRequestService;
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

	private IGSNRequestService igsnRequestService;

	public IGSNItemReader(IGSNRequestService igsnRequestService) {
		super();
		this.igsnRequestService = igsnRequestService;
	}

	@BeforeStep
	public void beforeStep(StepExecution stepExecution) {
		JobParameters jobParameters = stepExecution.getJobParameters();
		String IGSNServiceRequestID = jobParameters.getString("IGSNServiceRequestID");
		Request request = igsnRequestService.findById(IGSNServiceRequestID);
		this.filePath = request.getAttribute(Attribute.REQUESTED_IDENTIFIERS_PATH);
		init();
	}

	void init() {
		this.setName("IGSNItemReader");
		this.setResource(new FileSystemResource(new File(filePath)));
		this.setLineMapper(new PassThroughLineMapper());
	}

}
