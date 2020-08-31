package au.edu.ardc.igsn.extractor;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import au.edu.ardc.igsn.entity.Identifier;
import au.edu.ardc.igsn.model.Schema;
import au.edu.ardc.igsn.service.SchemaService;
import au.edu.ardc.igsn.util.XMLUtil;

public class XMLIdentifierExtractor implements IdentifierExtractor {

	@Override
	public String getIdentifier(Schema schema, String content){
		String identifierValue = "";
		if(schema.getId().equals(SchemaService.ARDCv1)) {
	        String xpath = "/resources/resource/resourceIdentifier";
			try {
				NodeList l  = XMLUtil.getXPath(content, xpath);
				if(l.getLength() > 0)
				{
					identifierValue = l.item(0).getFirstChild().getNodeValue();
				}
			} catch (XPathExpressionException | ParserConfigurationException | IOException | SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if(schema.getId().equals(SchemaService.CSIROv3)) {
	        String xpath = "/resources/resource/resourceIdentifier";
			try {
				NodeList l  = XMLUtil.getXPath(content, xpath);
				if(l.getLength() > 0)
				{
					identifierValue = l.item(0).getFirstChild().getNodeValue();
				}
			} catch (XPathExpressionException | ParserConfigurationException | IOException | SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return identifierValue;
	}
	
	
	

}
