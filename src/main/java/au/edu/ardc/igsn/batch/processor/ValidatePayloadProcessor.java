package au.edu.ardc.igsn.batch.processor;

import java.io.File;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

import au.edu.ardc.igsn.util.Helpers;
import au.edu.ardc.igsn.model.Schema;
import au.edu.ardc.igsn.service.SchemaService;
import au.edu.ardc.igsn.util.XMLUtil;

public class ValidatePayloadProcessor implements ItemProcessor<String, String> {
    
	@Autowired
    SchemaService service = new SchemaService();
    
	Logger logger = LoggerFactory.getLogger(ValidatePayloadProcessor.class);
	
    @Override
    public String process(String content) {
    	

    	try {
    		// validate a payload, schema, 
	    	boolean isValid = service.validate(content);  
	    	
	    	//User Ownership, 
	    	boolean isOwner = false;
	    	//igsn uniqueness,etc...
    	
    	}
    	catch( Exception e) {
    		logger.error(e.getMessage());
    	}
        return null;
    }
    

    	

}


