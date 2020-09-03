package au.edu.ardc.registry.igsn.validator;

import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.util.Helpers;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;

public class ContentValidator {

	@Autowired
	SchemaService service;

	public boolean validate(String content) throws Exception {
		return service.validate(content);
	}

	public boolean validate(Path path) throws Exception {
		String content = Helpers.readFile(path.toString());
		return service.validate(content);
	}

}
