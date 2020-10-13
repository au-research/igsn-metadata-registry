package au.edu.ardc.registry.job.processor;

import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.service.RecordService;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.service.VersionService;
import au.edu.ardc.registry.common.transform.Transformer;
import au.edu.ardc.registry.common.transform.TransformerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

public class RecordTransformOAIDCProcessor implements ItemProcessor<Record, Record> {

	private static final Logger logger = LoggerFactory.getLogger(RecordTransformOAIDCProcessor.class);

	protected final String defaultSchema = SchemaService.ARDCv1;

	private final VersionService versionService;

	private final SchemaService schemaService;

	private final RecordService recordService;

	public RecordTransformOAIDCProcessor(VersionService versionService, SchemaService schemaService,
			RecordService recordService) {
		this.versionService = versionService;
		this.schemaService = schemaService;
		this.recordService = recordService;
	}

	@Override
	public Record process(Record record) {
		logger.debug("Processing oai-dc transformation for recrd {}", record.getId());

		// obtain the version
		Version version = versionService.findVersionForRecord(record, defaultSchema);
		if (version == null) {
			logger.error("No valid version (with schema {}) found for record {}", defaultSchema, record.getId());
			return null;
		}
		logger.debug("version = {}", version.getId());

		// obtain the transformer
		Schema fromSchema = schemaService.getSchemaByID(defaultSchema);
		Schema toSchema = schemaService.getSchemaByID(SchemaService.OAIDC);
		Transformer transformer = (Transformer) TransformerFactory.create(fromSchema, toSchema);
		logger.debug("Transformer from {} to {} obtained", defaultSchema, SchemaService.OAIDC);

		try {
			Version oaiDCVersion = transformer.transform(version);
			oaiDCVersion.setHash(VersionService.getHash(oaiDCVersion));

			// check if there's existing current json-ld and if they're different
			Version existingVersion = versionService.findVersionForRecord(record, SchemaService.OAIDC);
			if (existingVersion != null && existingVersion.getHash().equals(oaiDCVersion.getHash())) {
				logger.info("There's already a version with existing hash {} for schema {}, skipping",
						existingVersion.getHash(), SchemaService.OAIDC);
				return null;
			}

			// save the newly created version
			// todo end previous version by record and schema
			versionService.save(oaiDCVersion);

			logger.info("Processed oai-dc transformation for record {}", record.getId());
			return recordService.findById(record.getId().toString());
		}
		catch (Exception e) {
			e.printStackTrace();
			logger.error("Error transforming json-ld for record = {} reason: {}", record.getId(), e.getMessage());
			return null;
		}
	}

}
