package au.edu.ardc.registry.common.util;

import au.edu.ardc.registry.exception.ContentNotSupportedException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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
		return (NodeList) xPath.compile(xpath).evaluate(xmlDocument, XPathConstants.NODESET);
	}

	public static String getNamespaceURI(String xml) throws ContentNotSupportedException {
		String nameSpace = "";
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			InputStream xmlStream = new ByteArrayInputStream(xml.getBytes());
			Document doc = builder.parse(xmlStream);
			Element root = doc.getDocumentElement();
			String rootPrefix = root.getPrefix();
			// the default namespace (no prefix)
			if (rootPrefix == null)
				rootPrefix = "xmlns";

			NamedNodeMap attributes = root.getAttributes();
			if (attributes != null) {
				for (int i = 0; i < attributes.getLength(); i++) {
					Node node = attributes.item(i);
					if (node.getNamespaceURI() == "http://www.w3.org/2000/xmlns/"
							&& node.getLocalName().equals(rootPrefix))
						return node.getNodeValue();
				}
			}
		}
		catch (Exception e) {
			throw new ContentNotSupportedException("Unable to determine namespace for given Document");
		}
		return nameSpace;
	}

}
