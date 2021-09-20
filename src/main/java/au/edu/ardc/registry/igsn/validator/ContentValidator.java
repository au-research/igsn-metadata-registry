package au.edu.ardc.registry.igsn.validator;

import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.exception.XMLValidationException;

import java.io.IOException;

public class ContentValidator {

	private final SchemaService service;

	public ContentValidator(SchemaService service) {
		this.service = service;
	}

	public boolean validate(String content) throws IOException, XMLValidationException {
		return service.validate(content);
	}

}
