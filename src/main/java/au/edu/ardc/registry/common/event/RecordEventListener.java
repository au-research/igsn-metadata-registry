package au.edu.ardc.registry.common.event;

import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.job.processor.RecordTitleProcessor;
import au.edu.ardc.registry.common.service.RecordService;
import au.edu.ardc.registry.common.service.VersionService;
import au.edu.ardc.registry.job.processor.RecordTransformLDProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class RecordEventListener {

	Logger logger = LoggerFactory.getLogger(RecordEventListener.class);

	@Autowired
	JobLauncher jobLauncher;

	@Autowired
	VersionService versionService;

	@Autowired
	RecordService recordService;

	@Autowired
	SchemaService schemaService;

	@Async
	@EventListener
	public void handleRecordUpdated(RecordUpdatedEvent event) throws InterruptedException {
		logger.debug("Event RecordUpdatedEvent raised with record {} and user {}", event.getRecord().getId(),
				event.getUser().getId());
		event.getRecord().setModifiedAt(new Date());
		event.getRecord().setModifierID(event.getUser().getId());

		// todo ProcessRecordJob (for single record)
		RecordTitleProcessor titleProcessor = new RecordTitleProcessor(versionService, recordService, schemaService);
		titleProcessor.process(event.getRecord());

		RecordTransformLDProcessor transformProcessor = new RecordTransformLDProcessor(versionService, recordService,
				schemaService);
		transformProcessor.process(event.getRecord());
	}

}
