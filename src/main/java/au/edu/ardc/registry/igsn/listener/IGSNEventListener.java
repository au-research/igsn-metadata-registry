package au.edu.ardc.registry.igsn.listener;

import au.edu.ardc.registry.common.event.RecordEventListener;
import au.edu.ardc.registry.common.event.RecordUpdatedEvent;
import au.edu.ardc.registry.common.service.RecordProcessingService;
import au.edu.ardc.registry.igsn.event.IGSNSyncedEvent;
import au.edu.ardc.registry.igsn.event.IGSNUpdatedEvent;
import au.edu.ardc.registry.igsn.service.IGSNService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class IGSNEventListener {

	Logger logger = LoggerFactory.getLogger(RecordEventListener.class);

	@Autowired
	IGSNService igsnService;

	@EventListener
	public void handleIGSNUpdatedEvent(IGSNUpdatedEvent event) {
		logger.debug("Event handleIGSNUpdatedEvent raised with identifier {} for request {}",
				event.getIdentifier().getValue(), event.getRequest().getId());

		igsnService.queueSync(event.getIdentifier(), event.getRequest());
	}

	@EventListener
	public void handleIGSNSyncedEvent(IGSNSyncedEvent event) {
		logger.debug("Event IGSNSyncedEvent raised with identifier {} for request {}", event.getIdentifier().getValue(),
				event.getRequest().getId());

		igsnService.checkRequest(event.getRequest());
	}

}