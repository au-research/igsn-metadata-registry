package au.edu.ardc.igsn.batch.processor;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import au.edu.ardc.igsn.util.Helpers;
import au.edu.ardc.igsn.model.Schema;
public class ValidatePayloadProcessor implements ItemProcessor<String, String> {
	
    Logger logger = LoggerFactory.getLogger(ValidatePayloadProcessor.class);
    
    @Override
    public String process(String s) {
    	
    	try {
    	String contentType = this.getContentType(s);
    	if(contentType.equals("application/xml")){
    		String namespace = this.getDefaultXMLnameSpace(s);	
        // todo validate a payload, schema, User Ownership, igsn uniqueness,etc...
    	}
    	}
    	catch( Exception e) {
    		logger.error(e.getMessage());
    	}
        return null;
    }
    
    public String getContentType(String content) throws Exception{
    	return Helpers.probeContentType(content);
    }
    
    public String getContentType(File file) throws Exception{
    	return Helpers.probeContentType(file);
    }
    
    public String getDefaultXMLnameSpace(String content) {
    	String xmlns = "";
    	
    	
    	return xmlns;
    }
    	
    
}


