package au.edu.ardc.registry.common.event;

import au.edu.ardc.registry.common.service.RecordProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class RecordEventListener {

	Logger logger = LoggerFactory.getLogger(RecordEventListener.class);

	@Autowired
	RecordProcessingService recordProcessingService;

	@Async
	@EventListener
	public void handleRecordUpdated(RecordUpdatedEvent event) {
		logger.debug("Event RecordUpdatedEvent raised with record {} and user {}", event.getRecord().getId(),
				event.getUser().getId());

		recordProcessingService.queueRecord(event.getRecord());
	}

}
