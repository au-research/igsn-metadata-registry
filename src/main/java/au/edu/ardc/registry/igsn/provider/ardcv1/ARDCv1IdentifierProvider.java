package au.edu.ardc.registry.igsn.provider.ardcv1;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import au.edu.ardc.registry.common.provider.IdentifierProvider;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.util.XMLUtil;

public class ARDCv1IdentifierProvider implements IdentifierProvider {

	@Override
	public String get(Schema schema, String content){
		String identifierValue = "";

		String xpath = "/resources/resource/resourceIdentifier";
		try {
			NodeList l  = XMLUtil.getXPath(content, xpath);
			if (l.getLength() > 0) {
				identifierValue = l.item(0).getFirstChild().getNodeValue();
			}
		} catch (XPathExpressionException | ParserConfigurationException | IOException | SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return identifierValue;
	}
	
	
	

}
