package au.edu.ardc.igsn.batch.processor;

import org.springframework.batch.item.ItemProcessor;

public class IngestProcessor implements ItemProcessor<String, String> {

    @Override
    public String process(String s) throws Exception {
        return s;
    }
}
