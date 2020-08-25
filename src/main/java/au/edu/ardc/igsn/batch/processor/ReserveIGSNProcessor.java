package au.edu.ardc.igsn.batch.processor;

import org.springframework.batch.item.ItemProcessor;

public class ReserveIGSNProcessor implements ItemProcessor<String, String> {

    @Override
    public String process(String identifier) {
        // todo create record[visible=False, Owner], identifier[state=RESERVED, type=IGSN]
        return identifier;
    }
}
