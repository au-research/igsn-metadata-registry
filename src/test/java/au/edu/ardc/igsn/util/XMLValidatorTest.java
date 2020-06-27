package au.edu.ardc.igsn.util;

import au.edu.ardc.igsn.TestHelper;
import org.junit.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class XMLValidatorTest {

    @Test
    public void it_can_validate_any_xml_given_an_xsd_path() throws IOException {

        // given an xml as a string
        String xml = Helpers.readFile("src/test/resources/xml/shiporder.xml");

        // it should validate against an xsd
        assertTrue(XMLValidator.validateXMLStringWithXSDPath(xml, "src/test/resources/schemas/shiporder.xsd"));
    }

}