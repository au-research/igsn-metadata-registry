package au.edu.ardc.registry.common.controller;

import au.edu.ardc.registry.exception.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Locale;

@RestControllerAdvice
public class APIRestControllerAdvice {

	@Autowired
	MessageSource messageSource;

	/**
	 * Handles all NotFound case of the API
	 * @param ex The RuntimeException that is encountered
	 * @param request the HttpServeletRequest, to display the path
	 * @return ResponseEntity
	 */
	@ExceptionHandler(value = { VersionNotFoundException.class })
	public ResponseEntity<Object> handleNotfound(APIException ex, HttpServletRequest request, Locale locale) {
		String message = messageSource.getMessage(ex.getMessageID(), ex.getArgs(), locale);
		APIExceptionResponse response = new APIExceptionResponse(message, HttpStatus.NOT_FOUND, request);

		return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
	}

	/**
	 * Handles forbidden 403 operations
	 * @param ex The RuntimeException that is encountered
	 * @param request the HttpServeletRequest, to display the path
	 * @return ResponseEntity
	 */
	@ExceptionHandler(value = { ForbiddenOperationException.class })
	public ResponseEntity<Object> handleForbidden(RuntimeException ex, HttpServletRequest request) {
		APIExceptionResponse response = new APIExceptionResponse(ex.getMessage());
		response.setTimestamp(new Date());
		response.setStatus(HttpStatus.FORBIDDEN.value());
		response.setError(HttpStatus.FORBIDDEN.toString());
		response.setPath(request.getServletPath());
		return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
	}

	@ExceptionHandler(value = { SchemaNotSupportedException.class, VersionContentAlreadyExisted.class })
	public ResponseEntity<Object> handleBadArgument(RuntimeException ex, HttpServletRequest request) {
		APIExceptionResponse response = new APIExceptionResponse(ex.getMessage());
		response.setTimestamp(new Date());
		response.setStatus(HttpStatus.BAD_REQUEST.value());
		response.setError(HttpStatus.BAD_REQUEST.toString());
		response.setPath(request.getServletPath());
		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	/**
	 * A global response for all Validation Exception
	 * @param ex handles MethodArgumentNotValidException
	 * @param request The current request
	 * @return APIExceptionResponse
	 */
	@ExceptionHandler(value = { MethodArgumentNotValidException.class })
	public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		APIExceptionResponse response = new APIExceptionResponse(ex.getMessage());
		response.setTimestamp(new Date());
		response.setStatus(HttpStatus.BAD_REQUEST.value());
		response.setError(HttpStatus.BAD_REQUEST.toString());
		response.setPath(request.getServletPath());

		// Map<String, String> errors = new HashMap<>();
		// ex.getBindingResult().getAllErrors().forEach((error) -> {
		// String fieldName = ((FieldError) error).getField();
		// String errorMessage = error.getDefaultMessage();
		// errors.put(fieldName, errorMessage);
		// });

		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

}
