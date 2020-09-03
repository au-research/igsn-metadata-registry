package au.edu.ardc.registry.igsn.job.processor;

import org.springframework.batch.item.ItemProcessor;

public class RegistrationProcessor implements ItemProcessor<String, String> {

	@Override
	public String process(String s) throws Exception {
		return s;
	}

}
