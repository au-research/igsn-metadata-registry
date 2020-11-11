package au.edu.ardc.registry.igsn.listener;

import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.event.RecordEventListener;
import au.edu.ardc.registry.common.event.RecordUpdatedEvent;
import au.edu.ardc.registry.common.model.Attribute;
import au.edu.ardc.registry.common.service.RecordProcessingService;
import au.edu.ardc.registry.igsn.event.IGSNSyncedEvent;
import au.edu.ardc.registry.igsn.event.IGSNUpdatedEvent;
import au.edu.ardc.registry.igsn.event.RequestExceptionEvent;
import au.edu.ardc.registry.igsn.service.IGSNService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.igsn.enabled")
public class IGSNEventListener {

	Logger logger = LoggerFactory.getLogger(RecordEventListener.class);

	@Autowired
	IGSNService igsnService;

	@EventListener
	public void handleIGSNUpdatedEvent(IGSNUpdatedEvent event) {
		Request request = event.getRequest();
		logger.debug("Event handleIGSNUpdatedEvent raised with identifier {} for request {}",
				event.getIdentifier().getValue(), request.getId());
		if(event.getIdentifier() != null){
			igsnService.queueSync(event.getIdentifier(), request);
		}
		igsnService.checkRequest(request);

	}

	@EventListener
	public void handleIGSNSyncedEvent(IGSNSyncedEvent event) {
		Request request = event.getRequest();
		logger.debug("Event IGSNSyncedEvent raised with identifier {} for request {}", event.getIdentifier().getValue(),
				request.getId());
		igsnService.checkRequest(request);
	}

	@EventListener
	public void handleIGSNSErrorEvent(RequestExceptionEvent event) {
		Request request = event.getRequest();
		logger.debug("Event RequestExceptionEvent raised with message {} for request {}", event.getMessage(),
				request.getId());
		igsnService.checkRequest(request);
	}

}
