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
	    	String contentType = this.getContentType(content);
	    	if(contentType.equals("application/xml")){
	    		boolean isValid = this.validate(content); 
    				
        // todo validate a payload, schema, User Ownership, igsn uniqueness,etc...
    	}
    	}
    	catch( Exception e) {
    		logger.error(e.getMessage());
    	}
        return null;
    }
    
    public boolean validate(String content) throws Exception {
		String nameSpace = this.getDefaultXMLnameSpace(content);
		Schema schema = this.service.getSchemaByNameSpace(nameSpace);
		return service.validate(schema, content);
    }
    
    public String getContentType(String content) throws Exception{
    	return Helpers.probeContentType(content);
    }
    
    public String getContentType(File file) throws Exception{
    	return Helpers.probeContentType(file);
    }
    
    public String getDefaultXMLnameSpace(String content) throws Exception {
    	String xmlns = XMLUtil.getNamespaceURI(content);
		logger.debug("DefaultXMLnameSpace:" + xmlns);
    	return xmlns;
    }
    	

}


