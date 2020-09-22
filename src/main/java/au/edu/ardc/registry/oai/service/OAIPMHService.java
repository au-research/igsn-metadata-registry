package au.edu.ardc.registry.oai.service;

import au.edu.ardc.registry.common.config.ApplicationProperties;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.common.service.RecordService;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.service.VersionService;
import au.edu.ardc.registry.oai.exception.BadVerbException;
import au.edu.ardc.registry.oai.exception.BadArgumentException;
import au.edu.ardc.registry.oai.exception.CannotDisseminateFormatException;
import au.edu.ardc.registry.oai.model.IdentifyFragment;
import au.edu.ardc.registry.oai.response.GetRecordResponse;
import au.edu.ardc.registry.oai.response.OAIIdentifyResponse;
import au.edu.ardc.registry.oai.response.OAIResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OAIPMHService {

	public static final int pageSize = 100;

	@Autowired
	RecordService recordService;

	@Autowired
	VersionService versionService;

	@Autowired
	ApplicationProperties applicationProperties;

	@Autowired
	SchemaService schemaService;

	public OAIResponse identify() {
		IdentifyFragment identify = new IdentifyFragment();
		identify.setRepositoryName(applicationProperties.getName());
		return new OAIIdentifyResponse(identify);
	}

	public OAIResponse getRecord(String metadataPrefix, String identifier) {
		if (metadataPrefix == null) {
			throw new BadArgumentException();
		}

		if (!schemaService.isOAIProvider(schemaService.getSchemaByID(metadataPrefix))) {
			throw new CannotDisseminateFormatException();
		}

		if (identifier == null) {
			throw new BadArgumentException();
		}

		try {
			Record record = recordService.findById(identifier);
			Version version = versionService.findVersionForRecord(record, metadataPrefix);
			String content = new String(version.getContent());
			return new GetRecordResponse(record, content);
		}
		catch (Exception e) {
			throw new BadVerbException("The value of the identifier argument is unknown or illegal in this repository.", "idDoesNotExist");
		}
	}

}
