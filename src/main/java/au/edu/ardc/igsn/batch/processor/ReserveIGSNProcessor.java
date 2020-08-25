package au.edu.ardc.igsn.batch.processor;

import au.edu.ardc.igsn.entity.Identifier;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.repository.IdentifierRepository;
import au.edu.ardc.igsn.repository.RecordRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

public class ReserveIGSNProcessor implements ItemProcessor<String, String> {

    RecordRepository recordRepository;

    IdentifierRepository identifierRepository;

    public ReserveIGSNProcessor(RecordRepository recordRepository, IdentifierRepository identifierRepository) {
        this.recordRepository = recordRepository;
        this.identifierRepository = identifierRepository;
    }

    @Override
    public String process(String identifierValue) {
        // todo create record[visible=False, Owner], identifier[state=RESERVED, type=IGSN]
        Record record = new Record();
        record.setCreatedAt(new Date());
        record = recordRepository.saveAndFlush(record);

        Identifier identifier = new Identifier();
        identifier.setCreatedAt(new Date());
        identifier.setRecord(record);
        identifier.setType(Identifier.Type.IGSN);
        identifier.setValue(identifierValue);
        identifier.setStatus(Identifier.Status.RESERVED);

        identifier = identifierRepository.saveAndFlush(identifier);

        return identifier.getId().toString();
    }
}
