package au.edu.ardc.registry.common.controller.api.admin;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@Tag(name = "Admin Operations")
@RequestMapping("/api/admin/process-records")
public class ProcessRecordController {

	@Autowired
	@Qualifier("queueJobLauncher")
	JobLauncher queue;

	@Autowired
	@Qualifier("standardJobLauncher")
	JobLauncher standardJobLauncher;

	@Autowired
	@Qualifier("ProcessRecordJob")
	Job ProcessRecordJob;

	@GetMapping("")
	public ResponseEntity<?> handle(@RequestParam(required = false, name = "method") String methodParam,
			@RequestParam(required = false) String id, @RequestParam(required = false, defaultValue = "0") boolean wait)
			throws JobParametersInvalidException, JobExecutionAlreadyRunningException, JobRestartException,
			JobInstanceAlreadyCompleteException {

		JobParametersBuilder paramBuilder = new JobParametersBuilder().addLong("time", System.currentTimeMillis());
		if (methodParam != null) {
			paramBuilder.addString("method", methodParam);
		}
		if (id != null) {
			paramBuilder.addString("id", id);
		}

		// @formatter:off
		JobExecution jobExecution = wait
				? standardJobLauncher.run(ProcessRecordJob, paramBuilder.toJobParameters())
				: queue.run(ProcessRecordJob, paramBuilder.toJobParameters());
		// @formatter:on

		Map<String, Object> result = new HashMap<>();
		result.put("id", jobExecution.getJobId());
		result.put("jobInstance", jobExecution.getJobInstance());
		result.put("jobParameters", jobExecution.getJobParameters());
		result.put("exitStatus", jobExecution.getExitStatus());
		result.put("failureExceptions", jobExecution.getAllFailureExceptions());

		return ResponseEntity.ok(result);
	}

}
