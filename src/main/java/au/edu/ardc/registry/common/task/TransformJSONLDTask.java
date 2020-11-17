package au.edu.ardc.registry.common.task;

import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.service.VersionService;
import au.edu.ardc.registry.common.transform.Transformer;
import au.edu.ardc.registry.common.transform.TransformerFactory;
import au.edu.ardc.registry.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class TransformJSONLDTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(TransformJSONLDTask.class);

	private final Record record;

	private final VersionService versionService;

	private final SchemaService schemaService;

	private final List<String> supportedSchemas = Arrays.asList(SchemaService.ARDCv1, SchemaService.CSIROv3);

	private final String outputSchemaID = SchemaService.JSONLD;

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
		// obtain the version
		// obtain latest version from supportd schemas
		// we need to support CSIROv3 and ARDCv1 at least !!
		Version version = null;

		for(String supportedSchema: supportedSchemas){
			Version v = versionService.findVersionForRecord(record, supportedSchema);
			if(v != null){
				if(version == null){
					version = v;
				}
				else if(version.getCreatedAt().before(v.getCreatedAt())){
					version = v;
				}
			}
		}

		if (version == null) {
			logger.error("Unable to generate registration metadata missing supported Schema version");
			throw new NotFoundException(
					"Unable to generate registration metadata missing supported Schema version");
		}

		Schema fromSchema = schemaService.getSchemaByID(version.getSchema());
		Schema toSchema = schemaService.getSchemaByID(outputSchemaID);
		Transformer transformer = (Transformer) TransformerFactory.create(fromSchema, toSchema);
		logger.debug("Transformer from {} to {} obtained", version.getSchema(), outputSchemaID);

		try {
			Version newVersion = transformer.transform(version);
			String hash = VersionService.getHash(newVersion);
			// check if there's existing current json-ld and if they're different
			Version existingVersion = versionService.findVersionForRecord(record, outputSchemaID);
			if (existingVersion != null) {
				if (!existingVersion.getHash().equals(hash)) {
					existingVersion.setHash(hash);
					existingVersion.setContent(newVersion.getContent());
					existingVersion.setCreatedAt(version.getCreatedAt());
					existingVersion.setRequestID(version.getRequestID());
					versionService.save(existingVersion);

				} else {
					logger.debug("There's already a version with existing hash {} for schema {}, skipping",
							existingVersion.getHash(), outputSchemaID);
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
