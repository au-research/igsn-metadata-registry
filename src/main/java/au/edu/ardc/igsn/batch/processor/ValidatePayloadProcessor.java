package au.edu.ardc.igsn.batch.processor;

import org.springframework.batch.item.ItemProcessor;

public class ValidatePayloadProcessor implements ItemProcessor<String, String> {

    @Override
    public String process(String s) {
        // todo validate a payload, schema, User Ownership, igsn uniqueness,etc...
        return null;
    }
}
