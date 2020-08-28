package au.edu.ardc.igsn.util;

import org.junit.Test;
import org.w3c.dom.NodeList;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class XMLUtilTest{


    @Test
    public void findDefaultXMLNamespace() throws Exception {
        String xml = Helpers.readFile("src/test/resources/xml/sample_igsn_csiro_v3.xml");
    	String nameSpace = XMLUtil.getNamespaceURI(xml);
    	assertTrue(nameSpace.equals("https://igsn.csiro.au/schemas/3.0"));
    }

    @Test
    public void getIdentifierFromCSIROXML() throws Exception{
        String xml = Helpers.readFile("src/test/resources/xml/sample_igsn_csiro_v3.xml");
        String xpath = "/resources/resource/resourceIdentifier";
        NodeList l = XMLUtil.getXPath(xml, xpath);
        String identifier = l.item(0).getFirstChild().getNodeValue();
        assertTrue(identifier.equals("CSTSTDOCO1"));
    }
    
    @Test
    public void getIdentifierFromCSIROWithPrefixXML() throws Exception{
        String xml = Helpers.readFile("src/test/resources/xml/sample_xml_prefix_cs_igsn.xml");
        String xpath = "/resources/resource/resourceIdentifier";
        NodeList l = XMLUtil.getXPath(xml, xpath);
        System.out.println(l.item(0).getFirstChild().getNodeValue());
        String identifier = l.item(0).getFirstChild().getNodeValue();
        assertTrue(identifier.equals("XXAA45CJ1N"));
    }
    
    @Test
    public void getIdentifierFromARDCXML() throws Exception{
        String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
        String xpath = "/resources/resource/resourceIdentifier";
        NodeList l = XMLUtil.getXPath(xml, xpath);
        System.out.println(l.item(0).getFirstChild().getNodeValue());
        String identifier = l.item(0).getFirstChild().getNodeValue();
        assertTrue(identifier.equals("10273/XX0TUIAYLV"));
    }
    
    @Test
    public void findprefixXMLNamespace() throws Exception {
        String xml = Helpers.readFile("src/test/resources/xml/sample_xml_prefix_cs_igsn.xml");
    	String nameSpace = XMLUtil.getNamespaceURI(xml);
    	assertTrue(nameSpace.equals("https://igsn.csiro.au/schemas/3.0"));
    }
}