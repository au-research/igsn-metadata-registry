package au.edu.ardc.registry.igsn.job.listener;

import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.igsn.service.IGSNRequestService;
import org.apache.logging.log4j.core.Logger;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;

import java.util.Date;

public class IGSNJobListener extends JobExecutionListenerSupport {

	IGSNRequestService igsnRequestService;

	private Logger logger;

	public IGSNJobListener(IGSNRequestService igsnRequestService) {
		super();
		this.igsnRequestService = igsnRequestService;
	}

	@Override
	public void beforeJob(JobExecution jobExecution) {
		Request request = getIGSNServiceRequest(jobExecution);
		this.logger = igsnRequestService.getLoggerFor(request);
		request.setStatus(Request.Status.RUNNING);
		request.setUpdatedAt(new Date());

		// todo provide more information about job here
		request.setMessage("Job started");

		igsnRequestService.save(request);
		logger.info("Job started");
		super.beforeJob(jobExecution);
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		Request request = getIGSNServiceRequest(jobExecution);
		this.logger = igsnRequestService.getLoggerFor(request);

		if (jobExecution.getExitStatus().equals(ExitStatus.FAILED)) {
			request.setMessage("Job Failed");
			logger.info("Job Failed");
			for (Throwable exception : jobExecution.getAllFailureExceptions()) {
				logger.error(exception.getMessage());
			}
		}

		request.setStatus(jobExecution.getExitStatus().equals(ExitStatus.FAILED) ? Request.Status.FAILED
				: Request.Status.COMPLETED);

		// todo provide more information about job message
		request.setMessage("Job Completed");
		request.setUpdatedAt(new Date());
		igsnRequestService.save(request);
		igsnRequestService.closeLoggerFor(request);
		super.afterJob(jobExecution);
	}

	private Request getIGSNServiceRequest(JobExecution jobExecution) {
		JobParameters parameters = jobExecution.getJobParameters();
		String IGSNServiceRequestID = parameters.getString("IGSNServiceRequestID");
		return igsnRequestService.findById(IGSNServiceRequestID);
	}

}
