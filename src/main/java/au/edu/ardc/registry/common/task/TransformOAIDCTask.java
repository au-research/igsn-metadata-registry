package au.edu.ardc.registry.common.task;

import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.service.VersionService;
import au.edu.ardc.registry.common.transform.Transformer;
import au.edu.ardc.registry.common.transform.TransformerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransformOAIDCTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(TransformOAIDCTask.class);

	private final Record record;

	private final VersionService versionService;

	private final SchemaService schemaService;

	public TransformOAIDCTask(Record record, VersionService versionService, SchemaService schemaService) {
		this.record = record;
		this.versionService = versionService;
		this.schemaService = schemaService;
	}

	@Override
	public void run() {
		// obtain the version
		Version version = versionService.findVersionForRecord(record, SchemaService.ARDCv1);
		if (version == null) {
			logger.error("No valid version (with schema {}) found for record {}", SchemaService.ARDCv1, record.getId());
			return;
		}

		// obtain the transformer
		Schema fromSchema = schemaService.getSchemaByID(SchemaService.ARDCv1);
		Schema toSchema = schemaService.getSchemaByID(SchemaService.OAIDC);
		Transformer transformer = (Transformer) TransformerFactory.create(fromSchema, toSchema);
		logger.debug("Transformer from {} to {} obtained", SchemaService.ARDCv1, SchemaService.OAIDC);

		try {
			Version oaiDCVersion = transformer.transform(version);
			oaiDCVersion.setHash(VersionService.getHash(oaiDCVersion));

			// check if there's existing current json-ld and if they're different
			Version existingVersion = versionService.findVersionForRecord(record, SchemaService.OAIDC);
			if (existingVersion != null && existingVersion.getHash().equals(oaiDCVersion.getHash())) {
				logger.info("There's already a version with existing hash {} for schema {}, skipping",
						existingVersion.getHash(), SchemaService.OAIDC);
				return;
			}

			// save the newly created version
			// todo end previous version by record and schema
			versionService.save(oaiDCVersion);

			logger.info("Processed oai-dc transformation for record {}", record.getId());
		}
		catch (Exception e) {
			e.printStackTrace();
			logger.error("Error transforming json-ld for record = {} reason: {}", record.getId(), e.getMessage());
		}
	}

}