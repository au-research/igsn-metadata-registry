package au.edu.ardc.registry.igsn.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConfigurationProperties(prefix = "app.igsn")
@ConditionalOnProperty(name = "app.igsn.enabled")
public class IGSNApplicationConfig {

	private boolean disableAutomaticQueueWorkerInit = false;

	public boolean isDisableAutomaticQueueWorkerInit() {
		return disableAutomaticQueueWorkerInit;
	}

	public void setDisableAutomaticQueueWorkerInit(boolean disableAutomaticQueueWorkerInit) {
		this.disableAutomaticQueueWorkerInit = disableAutomaticQueueWorkerInit;
	}

}
