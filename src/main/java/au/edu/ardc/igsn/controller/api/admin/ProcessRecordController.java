package au.edu.ardc.igsn.controller.api.admin;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/process-records")
public class ProcessRecordController {

    @Autowired
    @Qualifier("asyncJobLauncher")
    JobLauncher asyncJobLauncher;

    @Autowired
    Job ProcessRecordJob;

    @GetMapping("")
    public String handle(
            @RequestParam(required = false, name="method") String methodParam
    )
            throws JobParametersInvalidException
            , JobExecutionAlreadyRunningException
            , JobRestartException
            , JobInstanceAlreadyCompleteException {
        String method = "findAll";
        if (methodParam != null) {
            method = methodParam;
        }

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("method", method)
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        asyncJobLauncher.run(ProcessRecordJob, jobParameters);

        return "batch Job is invoked!";
    }

}
