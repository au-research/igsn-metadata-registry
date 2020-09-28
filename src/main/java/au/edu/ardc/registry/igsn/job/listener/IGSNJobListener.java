package au.edu.ardc.registry.igsn.job.listener;

import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.igsn.service.IGSNRequestService;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;

import java.util.Date;

public class IGSNJobListener extends JobExecutionListenerSupport {

	IGSNRequestService igsnService;

	public IGSNJobListener(IGSNRequestService igsnService) {
		super();
		this.igsnService = igsnService;
	}

	@Override
	public void beforeJob(JobExecution jobExecution) {
		Request request = getIGSNServiceRequest(jobExecution);
		request.setStatus(Request.Status.RUNNING);
		request.setUpdatedAt(new Date());
		igsnService.save(request);
		igsnService.getLoggerFor(request).info("Job started");
		super.beforeJob(jobExecution);
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		Request request = getIGSNServiceRequest(jobExecution);

		if (jobExecution.getExitStatus().equals(ExitStatus.FAILED)) {
			igsnService.getLoggerFor(request).info("Job Failed");
			for (Throwable exception : jobExecution.getAllFailureExceptions()) {
				igsnService.getLoggerFor(request).severe(exception.getMessage());
			}
		}

		request.setStatus(jobExecution.getExitStatus().equals(ExitStatus.FAILED) ? Request.Status.FAILED
				: Request.Status.COMPLETED);

		request.setUpdatedAt(new Date());
		igsnService.save(request);
		igsnService.closeLoggerFor(request);
		super.afterJob(jobExecution);
	}

	private Request getIGSNServiceRequest(JobExecution jobExecution) {
		JobParameters parameters = jobExecution.getJobParameters();
		String IGSNServiceRequestID = parameters.getString("IGSNServiceRequestID");
		return igsnService.findById(IGSNServiceRequestID);
	}

}
