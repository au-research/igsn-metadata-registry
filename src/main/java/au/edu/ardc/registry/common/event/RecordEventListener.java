package au.edu.ardc.registry.common.event;

import au.edu.ardc.registry.common.service.RecordService;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.service.VersionService;
import au.edu.ardc.registry.job.processor.RecordTitleProcessor;
import au.edu.ardc.registry.job.processor.RecordTransformLDProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class RecordEventListener {

	Logger logger = LoggerFactory.getLogger(RecordEventListener.class);

	@Autowired
	@Qualifier("ProcessRecordJob")
	Job ProcessRecordJob;

	@Autowired
	@Qualifier("queueJobLauncher")
	JobLauncher queue;

	@Async
	@EventListener
	public void handleRecordUpdated(RecordUpdatedEvent event) throws JobParametersInvalidException, JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {
		logger.debug("Event RecordUpdatedEvent raised with record {} and user {}", event.getRecord().getId(),
				event.getUser().getId());
		event.getRecord().setModifiedAt(new Date());
		event.getRecord().setModifierID(event.getUser().getId());

		// trigger a new job
		JobParametersBuilder paramBuilder = new JobParametersBuilder().addString("method", "findById")
				.addString("id", event.getRecord().getId().toString()).addLong("time", System.currentTimeMillis());

		queue.run(ProcessRecordJob, paramBuilder.toJobParameters());
	}

}
