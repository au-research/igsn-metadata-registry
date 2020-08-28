package au.edu.ardc.igsn.util;

import org.junit.Test;

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
    public void findprefixXMLNamespace() throws Exception {
        String xml = Helpers.readFile("src/test/resources/xml/sample_xml_prefix_cs_igsn.xml");
    	String nameSpace = XMLUtil.getNamespaceURI(xml);
    	assertTrue(nameSpace.equals("https://igsn.csiro.au/schemas/3.0"));
    }
}