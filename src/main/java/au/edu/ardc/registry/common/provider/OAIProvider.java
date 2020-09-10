package au.edu.ardc.registry.common.provider;

import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.service.SchemaService;
import org.springframework.beans.factory.annotation.Autowired;

import static au.edu.ardc.registry.common.service.SchemaService.*;

public class OAIProvider {

	@Autowired
	SchemaService schemaService;

	public String getNamespace(Schema schema) {
		return schema.getNamespace();
	};

	public String getPrefix(Schema schema) {
		return schema.getId();
	};

	public String getFormatSchema(Schema schema) {
		return schema.getSchemaLocation();
	};

}
