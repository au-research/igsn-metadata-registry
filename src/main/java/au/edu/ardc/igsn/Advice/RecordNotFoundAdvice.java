package au.edu.ardc.igsn.Advice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import au.edu.ardc.igsn.Exceptions.RecordNotFoundException;


@ControllerAdvice
public class RecordNotFoundAdvice {

	@Autowired
	ObjectMapper mapper;
	
  @ResponseBody
  @ExceptionHandler(RecordNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  ObjectNode employeeNotFoundHandler(RecordNotFoundException ex) throws Exception {
	String context = "igsn/record/{id}";
	ObjectNode objectNode = mapper.createObjectNode();
	objectNode.put("error", ex.getMessage());
	objectNode.put("context", context);
	return objectNode;
	
	//return JsonUtils.convertToJSON(ex, context);
	  }
	}
