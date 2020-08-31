package au.edu.ardc.registry.job.processor;

import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.common.service.RecordService;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.service.VersionService;
import au.edu.ardc.registry.common.util.XMLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class RecordTitleProcessor implements ItemProcessor<Record, Record> {

    protected final String defaultSchema = SchemaService.ARDCv1;
    private final VersionService versionService;
    private final RecordService recordService;
    Logger logger = LoggerFactory.getLogger(RecordTitleProcessor.class);

    public RecordTitleProcessor(VersionService versionService, RecordService recordService) {
        this.versionService = versionService;
        this.recordService = recordService;
    }

    @Override
    public Record process(Record record) throws InterruptedException {
        logger.debug("Processing title for record {} ", record.getId());

        //Thread.sleep(2000);

        // obtain the version
        Version version = versionService.findVersionForRecord(record, defaultSchema);
        if (version == null) {
            logger.error("No valid version found for record {}", record.getId());
            return null;
        }

        // obtain the xml from the version
        String xml = new String(version.getContent());

        // obtain the title from the xml
        String title;
        try {
            NodeList nodeList = XMLUtil.getXPath(xml, "//resourceTitle");
            Node resourceTitleNode = nodeList.item(0);
            title = resourceTitleNode.getTextContent();
        } catch (Exception ex) {
            logger.error("Failed obtaining title for record {} from XML", record.getId());
            ex.printStackTrace();
            return null;
        }
        logger.debug("Found title {} for record {}", title, record.getId());

        // save the title on the record
        record.setTitle(title);
        record = recordService.save(record);

        logger.debug("Processed title successful for record {} ", record.getId());
        return record;
    }
}
