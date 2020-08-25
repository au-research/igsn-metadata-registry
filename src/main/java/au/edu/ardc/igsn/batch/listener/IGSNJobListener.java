package au.edu.ardc.igsn.batch.listener;

import au.edu.ardc.igsn.entity.IGSNServiceRequest;
import au.edu.ardc.igsn.service.IGSNService;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;

import java.util.Date;

public class IGSNJobListener extends JobExecutionListenerSupport {

    IGSNService igsnService;

    public IGSNJobListener(IGSNService igsnService) {
        super();
        this.igsnService = igsnService;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        IGSNServiceRequest request = getIGSNServiceRequest(jobExecution);
        request.setStatus(IGSNServiceRequest.Status.RUNNING);
        request.setUpdatedAt(new Date());
        igsnService.save(request);
        igsnService.getLoggerFor(request).info("Job started");
        super.beforeJob(jobExecution);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        IGSNServiceRequest request = getIGSNServiceRequest(jobExecution);
        request.setStatus(IGSNServiceRequest.Status.COMPLETED);
        request.setUpdatedAt(new Date());
        igsnService.save(request);
        igsnService.getLoggerFor(request).info("Job Finished");
        igsnService.closeLoggerFor(request);
        super.afterJob(jobExecution);
    }

    private IGSNServiceRequest getIGSNServiceRequest(JobExecution jobExecution) {
        JobParameters parameters = jobExecution.getJobParameters();
        String IGSNServiceRequestID = parameters.getString("IGSNServiceRequestID");
        return igsnService.findById(IGSNServiceRequestID);
    }

}
