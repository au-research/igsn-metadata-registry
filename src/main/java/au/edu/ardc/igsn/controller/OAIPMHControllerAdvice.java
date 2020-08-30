package au.edu.ardc.igsn.controller;

import au.edu.ardc.igsn.exception.oai.BadVerbException;
import au.edu.ardc.igsn.oai.model.RequestFragment;
import au.edu.ardc.igsn.oai.response.OAIExceptionResponse;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.Date;

@ControllerAdvice
public class OAIPMHControllerAdvice {

    @ExceptionHandler(value = {BadVerbException.class})
    public ResponseEntity<Object> handleBadVerb(RuntimeException ex, HttpServletRequest request) throws XMLStreamException, IOException {
        OAIExceptionResponse response = new OAIExceptionResponse();
        RequestFragment requestFragment = new RequestFragment();
        requestFragment.setValue(request.getRequestURL().toString());
        response.setRequest(requestFragment);
        response.setResponseDate(new Date());
        response.setError(ex.getMessage());

        // add xml declaration
        XmlMapper mapper = new XmlMapper();
        mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
        String xml = mapper.writeValueAsString(response);

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_XML)
                .body(xml);
    }

}
