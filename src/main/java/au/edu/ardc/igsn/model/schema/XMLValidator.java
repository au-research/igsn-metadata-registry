package au.edu.ardc.igsn.model.schema;

import au.edu.ardc.igsn.exception.XMLValidationException;
import au.edu.ardc.igsn.model.Schema;
import au.edu.ardc.igsn.util.XMLUtil;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

public class XMLValidator implements SchemaValidator {
    public boolean validate(Schema schema, String xmlString) {

        XMLSchema xmlSchema = (XMLSchema) schema;
        String XSDPath = xmlSchema.getLocalSchemaLocation();
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            File schemaFile = new File(XSDPath);
            javax.xml.validation.Schema validationSchema = factory.newSchema(schemaFile);
            Validator validator = validationSchema.newValidator();

            StringReader reader = new StringReader(xmlString);
            validator.validate(new StreamSource(reader));

            return true;
        } catch (IOException | SAXException e) {
            throw new XMLValidationException(e.getMessage());
        }
    }
}
