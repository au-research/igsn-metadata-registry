package au.edu.ardc.registry.common.task;

import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.provider.Metadata;
import au.edu.ardc.registry.common.provider.MetadataProviderFactory;
import au.edu.ardc.registry.common.provider.TitleProvider;
import au.edu.ardc.registry.common.service.RecordService;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.service.VersionService;
import au.edu.ardc.registry.common.transform.Transformer;
import au.edu.ardc.registry.common.transform.TransformerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessRecordTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(ProcessRecordTask.class);

	private final Record record;

	private final VersionService versionService;

	private final RecordService recordService;

	private final SchemaService schemaService;

	public ProcessRecordTask(Record record, VersionService versionService, RecordService recordService,
			SchemaService schemaService) {
		this.record = record;
		this.versionService = versionService;
		this.recordService = recordService;
		this.schemaService = schemaService;
	}

	@Override
	public void run() {
		processTitle(record);
		processTransformJSONLD(record);
		processTransformOAIDC(record);
	}

	/**
	 * Process Title. Only process ardcv1 titles at the moment. Should be refactored to
	 * handle other schemas todo refactor to some service class (probably)
	 * @since 1.0
	 * @param record the {@link Record} to process the title
	 */
	private void processTitle(Record record) {
		// process title
		Version version = versionService.findVersionForRecord(record, SchemaService.ARDCv1);
		if (version == null) {
			logger.error("No valid version found for record {}", record.getId());
			return;
		}

		// obtain the title using the TitleProvider
		String xml = new String(version.getContent());
		Schema schema = schemaService.getSchemaByID(version.getSchema());
		TitleProvider provider = (TitleProvider) MetadataProviderFactory.create(schema, Metadata.Title);
		String title = provider.get(xml);

		record.setTitle(title);
		recordService.save(record);
	}

	private void processTransformJSONLD(Record record) {
		Version version = versionService.findVersionForRecord(record, SchemaService.ARDCv1);
		if (version == null) {
			logger.error("No valid version (with schema {}) found for record {}", SchemaService.ARDCv1, record.getId());
			return;
		}

		// obtain the transformer
		Schema fromSchema = schemaService.getSchemaByID(SchemaService.ARDCv1);
		Schema toSchema = schemaService.getSchemaByID(SchemaService.ARDCv1JSONLD);
		Transformer transformer = (Transformer) TransformerFactory.create(fromSchema, toSchema);
		logger.debug("Transformer from {} to {} obtained", SchemaService.ARDCv1, SchemaService.ARDCv1JSONLD);

		try {
			Version jsonLDVersion = transformer.transform(version);
			jsonLDVersion.setHash(VersionService.getHash(jsonLDVersion));

			// check if there's existing current json-ld and if they're different
			Version existingVersion = versionService.findVersionForRecord(record, SchemaService.ARDCv1JSONLD);
			if (existingVersion != null && existingVersion.getHash().equals(jsonLDVersion.getHash())) {
				logger.info("There's already a version with existing hash {} for schema {}, skipping",
						existingVersion.getHash(), SchemaService.ARDCv1JSONLD);
				return;
			}

			// save the newly created version
			versionService.save(jsonLDVersion);

			logger.info("Processed json-ld transformation for record {}", record.getId());
		}
		catch (Exception e) {
			e.printStackTrace();
			logger.error("Error transforming json-ld for record = {} reason: {}", record.getId(), e.getMessage());
		}
	}

	private void processTransformOAIDC(Record record) {
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
