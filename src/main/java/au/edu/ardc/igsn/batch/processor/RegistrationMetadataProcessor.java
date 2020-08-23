package au.edu.ardc.igsn.batch.processor;

import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.entity.Version;
import au.edu.ardc.igsn.service.RecordService;
import au.edu.ardc.igsn.service.SchemaService;
import au.edu.ardc.igsn.service.VersionService;
import au.edu.ardc.igsn.util.XMLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class RegistrationMetadataProcessor implements ItemProcessor<Record, Record> {

    protected final String defaultSchema = SchemaService.ARDCv1;
    private final VersionService versionService;
    private final RecordService recordService;
    Logger logger = LoggerFactory.getLogger(RecordTitleProcessor.class);

    public RegistrationMetadataProcessor(VersionService versionService, RecordService recordService) {
        this.versionService = versionService;
        this.recordService = recordService;
    }

    @Override
    public Record process(Record record) {
        logger.debug("Generating Registration Metadata for record {} ", record.getId());

        // obtain the version
        Version version = versionService.findVersionForRecord(record, defaultSchema);
        if (version == null) {
            logger.error("No valid version found for record {}", record.getId());
            return null;
        }

        // obtain the xml from the version
        String xml = new String(version.getContent());

        String registration_metadata = "";
        try {
        	// TODO ADD XSLT TRansform 
        } catch (Exception ex) {
            logger.error("Failed to Generate Registration Metadata from content: ", xml);
            ex.printStackTrace();
            return null;
        }
        logger.debug("Registration Metadata:  {} for record {}", registration_metadata, record.getId());
        	// TODO saved the result as a registration metadata in the version

        logger.debug("Finished Generating Registration Metadata for record {} ", record.getId());

        return record;
    }
}