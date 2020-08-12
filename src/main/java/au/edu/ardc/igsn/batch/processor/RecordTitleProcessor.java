package au.edu.ardc.igsn.batch.processor;

import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.entity.Version;
import au.edu.ardc.igsn.service.RecordService;
import au.edu.ardc.igsn.service.VersionService;
import au.edu.ardc.igsn.util.XMLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class RecordTitleProcessor implements ItemProcessor<Record, Record> {
    private final VersionService versionService;
    private final RecordService recordService;
    Logger logger = LoggerFactory.getLogger(RecordTitleProcessor.class);

    public RecordTitleProcessor(VersionService versionService, RecordService recordService) {
        this.versionService = versionService;
        this.recordService = recordService;
    }

    @Override
    public Record process(Record record) {
        logger.debug("Processing record: " + record.getId());

        // obtain the version
        Version version = versionService.findVersionForRecord(record, "igsn-csiro-v3-descriptive");
        if (version == null) {
            logger.error(String.format("No valid version found for record %s", record.getId()));
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
            logger.error("Failed obtaining title for record %s " + record.getId());
            logger.error("Failed obtaining title from xml: " + xml);
            ex.printStackTrace();
            return null;
        }
        logger.debug(String.format("Found Title: %s for Record %s", title, record.getId()));

        // save the title on the record
        record.setTitle(title);
        record = recordService.save(record);

        logger.debug("Processed title successful for record: " + record.getId());
        return record;
    }
}
