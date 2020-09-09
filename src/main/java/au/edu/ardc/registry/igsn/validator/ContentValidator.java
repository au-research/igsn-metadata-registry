package au.edu.ardc.registry.igsn.validator;

import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.exception.ContentNotSupportedException;
import au.edu.ardc.registry.exception.XMLValidationException;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Path;

public class ContentValidator {

	private final SchemaService service;

	public ContentValidator(SchemaService service) {
		this.service = service;
	}

	public Schema getSchema(String content) throws ContentNotSupportedException {
		return service.getSchemaForContent(content);
	}

	public boolean validate(String content) throws IOException, XMLValidationException {
		return service.validate(content);
	}

	public boolean validate(Path path) throws IOException, XMLValidationException {
		String content = Helpers.readFile(path.toString());
		return service.validate(content);
	}

}
