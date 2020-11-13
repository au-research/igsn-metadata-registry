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

public class TransformJSONLDTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(TransformJSONLDTask.class);

	private final Record record;

	private final VersionService versionService;

	private final SchemaService schemaService;

	public TransformJSONLDTask(Record record, VersionService versionService, SchemaService schemaService) {
		this.record = record;
		this.versionService = versionService;
		this.schemaService = schemaService;
	}

	/**
	 * Generate a new JSON-LD version for the record. Supports ardcv1
	 */
	@Override
	public void run() {
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
			Version newVersion = transformer.transform(version);
			String hash = VersionService.getHash(newVersion);
			// check if there's existing current json-ld and if they're different
			Version existingVersion = versionService.findVersionForRecord(record, SchemaService.ARDCv1JSONLD);
			if (existingVersion != null) {
				if (!existingVersion.getHash().equals(hash)) {
					existingVersion.setHash(hash);
					existingVersion.setContent(newVersion.getContent());
					existingVersion.setCreatedAt(version.getCreatedAt());
					existingVersion.setRequestID(version.getRequestID());
					versionService.save(existingVersion);

				} else {
					logger.debug("There's already a version with existing hash {} for schema {}, skipping",
							existingVersion.getHash(), SchemaService.ARDCv1JSONLD);
					return;
				}
			}else{
				newVersion.setRequestID(version.getRequestID());
				newVersion.setCreatedAt(version.getCreatedAt());
				newVersion.setHash(hash);
				versionService.save(newVersion);
			}

			logger.info("Processed json-ld transformation for record {}", record.getId());
		}
		catch (Exception e) {
			e.printStackTrace();
			logger.error("Error transforming json-ld for record = {} reason: {}", record.getId(), e.getMessage());
		}
	}

}
