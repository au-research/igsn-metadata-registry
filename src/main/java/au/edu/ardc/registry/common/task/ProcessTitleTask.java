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
import au.edu.ardc.registry.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class ProcessTitleTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(ProcessTitleTask.class);

	private final Record record;

	private final VersionService versionService;

	private final RecordService recordService;

	private final SchemaService schemaService;

	private final List<String> supportedSchemas = Arrays.asList(SchemaService.ARDCv1, SchemaService.CSIROv3);

	public ProcessTitleTask(Record record, VersionService versionService, RecordService recordService,
			SchemaService schemaService) {
		this.record = record;
		this.versionService = versionService;
		this.recordService = recordService;
		this.schemaService = schemaService;
	}

	/**
	 * Process the title of the record. Only process ardcv1 schema at the moment
	 */
	@Override
	public void run() {
		// process title
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

}