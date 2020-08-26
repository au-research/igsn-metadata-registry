package au.edu.ardc.igsn.util;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;

public class XMLUtil {

    public static NodeList getXPath(String xml, String xpath)
            throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        InputStream xmlStream = new ByteArrayInputStream(xml.getBytes());

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        Document xmlDocument = builder.parse(xmlStream);

        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodeList = (NodeList) xPath.compile(xpath).evaluate(xmlDocument, XPathConstants.NODESET);

        return nodeList;
    }
    
    
    public static String getNamespaceURI(String xml) throws Exception {
    	
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    factory.setNamespaceAware(true);
	    DocumentBuilder builder = factory.newDocumentBuilder();
	    Document doc = builder.parse(xml);
	    Element root = doc.getDocumentElement();
        String name = "";
        String nameSpace = "";
	    NamedNodeMap attributes = root.getAttributes();
	    if (attributes != null)
	    {
	        for (int i = 0; i < attributes.getLength(); i++)
	        {
	            Node node = attributes.item(i);
	            if (node.getNodeType() == Node.ATTRIBUTE_NODE)
	            {
	                name = node.getNodeName();
	                nameSpace = node.getNamespaceURI();
	                System.out.println(name + " " + node.getNamespaceURI());
	            }
	        }
	    }
	    return nameSpace;
    }
    
    
}
