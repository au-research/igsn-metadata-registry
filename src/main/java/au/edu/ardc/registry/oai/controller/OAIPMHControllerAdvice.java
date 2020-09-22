package au.edu.ardc.registry.oai.controller;

import au.edu.ardc.registry.exception.APIException;
import au.edu.ardc.registry.oai.exception.BadArgumentException;
import au.edu.ardc.registry.oai.exception.BadVerbException;
import au.edu.ardc.registry.oai.model.ErrorFragment;
import au.edu.ardc.registry.oai.model.RequestFragment;
import au.edu.ardc.registry.oai.response.OAIExceptionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;

@ControllerAdvice
public class OAIPMHControllerAdvice {

	@Autowired
	MessageSource messageSource;

	@ExceptionHandler(value = { BadVerbException.class })
	public ResponseEntity<Object> handleBadVerb(RuntimeException ex, HttpServletRequest request)
			throws XMLStreamException, IOException {

		OAIExceptionResponse response = new OAIExceptionResponse();
		ErrorFragment errorFragment = new ErrorFragment();
		errorFragment.setValue(ex.getMessage());
		errorFragment.setCode(BadVerbException.getCode());
		RequestFragment requestFragment = new RequestFragment();
		requestFragment.setValue(request.getRequestURL().toString());
		response.setRequest(requestFragment);
		response.setResponseDate(new Date());
		response.setError(errorFragment);

		// add xml declaration
		XmlMapper mapper = new XmlMapper();
		mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
		String xml = mapper.writeValueAsString(response);

		return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_XML).body(xml);
	}

	@ExceptionHandler(value = { BadArgumentException.class })
	public ResponseEntity<Object> handleBadVerb(APIException ex, HttpServletRequest request, Locale locale)
			throws JsonProcessingException {

		OAIExceptionResponse response = new OAIExceptionResponse();

		ErrorFragment errorFragment = new ErrorFragment();
		String message = messageSource.getMessage(ex.getMessageID(), ex.getArgs(), locale);
		errorFragment.setValue(message);
		errorFragment.setCode(BadVerbException.getCode());

		RequestFragment requestFragment = new RequestFragment();
		requestFragment.setValue(request.getRequestURL().toString());
		response.setRequest(requestFragment);
		response.setResponseDate(new Date());
		response.setError(errorFragment);

		// add xml declaration
		XmlMapper mapper = new XmlMapper();
		mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
		// todo try catch here
		String xml = mapper.writeValueAsString(response);

		return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_XML).body(xml);
	}

}
