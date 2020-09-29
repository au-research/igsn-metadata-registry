package au.edu.ardc.registry.igsn.job.listener;

import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.service.RequestService;
import au.edu.ardc.registry.igsn.service.IGSNRequestService;
import org.apache.logging.log4j.core.Logger;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;

import java.util.Date;

public class IGSNJobListener extends JobExecutionListenerSupport {

	IGSNRequestService igsnService;

	RequestService requestService;

	private Logger logger;

	public IGSNJobListener(IGSNRequestService igsnService, RequestService requestService) {
		super();
		this.igsnService = igsnService;
		this.requestService = requestService;
	}

	@Override
	public void beforeJob(JobExecution jobExecution) {
		Request request = getIGSNServiceRequest(jobExecution);
		this.logger = requestService.getLoggerFor(request);
		request.setStatus(Request.Status.RUNNING);
		request.setUpdatedAt(new Date());
		igsnService.save(request);
		logger.info("Job started");
		super.beforeJob(jobExecution);
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		Request request = getIGSNServiceRequest(jobExecution);
		this.logger = requestService.getLoggerFor(request);

		if (jobExecution.getExitStatus().equals(ExitStatus.FAILED)) {
			logger.info("Job Failed");
			for (Throwable exception : jobExecution.getAllFailureExceptions()) {
				logger.error(exception.getMessage());
			}
		}

		request.setStatus(jobExecution.getExitStatus().equals(ExitStatus.FAILED) ? Request.Status.FAILED
				: Request.Status.COMPLETED);

		request.setUpdatedAt(new Date());
		igsnService.save(request);
		requestService.closeLoggerFor(request);
		super.afterJob(jobExecution);
	}

	private Request getIGSNServiceRequest(JobExecution jobExecution) {
		JobParameters parameters = jobExecution.getJobParameters();
		String IGSNServiceRequestID = parameters.getString("IGSNServiceRequestID");
		return igsnService.findById(IGSNServiceRequestID);
	}

}
