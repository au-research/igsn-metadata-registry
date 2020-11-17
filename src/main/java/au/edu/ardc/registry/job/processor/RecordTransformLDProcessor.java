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

/**
 * todo refactor Tests into RecordProcesssingTask.processTransform and remove this class
 */
public class RecordTransformLDProcessor implements ItemProcessor<Record, Record> {

	protected final String defaultSchema = SchemaService.ARDCv1;

	private final VersionService versionService;

	private final RecordService recordService;

	private final SchemaService schemaService;

	Logger log = LoggerFactory.getLogger(RecordTransformLDProcessor.class);

	public RecordTransformLDProcessor(VersionService versionService, RecordService recordService,
			SchemaService schemaService) {
		this.versionService = versionService;
		this.recordService = recordService;
		this.schemaService = schemaService;
	}

	@Override
	public Record process(Record record) {
		log.debug("Processing json-ld transformation for record {}", record.getId());

		// obtain the version
		Version version = versionService.findVersionForRecord(record, defaultSchema);
		if (version == null) {
			log.error("No valid version (with schema {}) found for record {}", defaultSchema, record.getId());
			return null;
		}
		log.debug("version = {}", version.getId());

		// obtain the transformer
		Schema fromSchema = schemaService.getSchemaByID(defaultSchema);
		Schema toSchema = schemaService.getSchemaByID(SchemaService.JSONLD);
		Transformer transformer = (Transformer) TransformerFactory.create(fromSchema, toSchema);
		if (transformer == null) {
			log.error("Failed to obtain a transformer from {} to {}", defaultSchema, SchemaService.JSONLD);
			return null;
		}
		log.debug("Transformer from {} to {} obtained", defaultSchema, SchemaService.JSONLD);

		// perform the transform
		try {
			Version jsonLDVersion = transformer.transform(version);
			jsonLDVersion.setHash(versionService.getHash(jsonLDVersion));

			// check if there's existing current json-ld and if they're different
			Version existingVersion = versionService.findVersionForRecord(record, SchemaService.JSONLD);
			if (existingVersion != null && existingVersion.getHash().equals(jsonLDVersion.getHash())) {
				log.info("There's already a version with existing hash {} for schema {}, skipping",
						existingVersion.getHash(), SchemaService.JSONLD);
				return recordService.findById(record.getId().toString());
			}

			// save the newly created version
			versionService.save(jsonLDVersion);

			// todo recordService.touch(record, user) for historical detail
			log.info("Processed json-ld transformation for record {}", record.getId());
			return recordService.findById(record.getId().toString());
		}
		catch (Exception e) {
			e.printStackTrace();
			log.error("Error transforming json-ld for record = {} reason: {}", record.getId(), e.getMessage());
			return null;
		}
	}

}
