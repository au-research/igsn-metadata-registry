package au.edu.ardc.registry.igsn.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConfigurationProperties(prefix = "app.igsn")
public class IGSNApplicationConfig {

	private boolean disableAutomaticQueueWorkerInit = false;

	public boolean isDisableAutomaticQueueWorkerInit() {
		return disableAutomaticQueueWorkerInit;
	}

	public void setDisableAutomaticQueueWorkerInit(boolean disableAutomaticQueueWorkerInit) {
		this.disableAutomaticQueueWorkerInit = disableAutomaticQueueWorkerInit;
	}

}
