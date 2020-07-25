package au.edu.ardc.igsn.controller;

import au.edu.ardc.igsn.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class APIRestControllerAdvice {

    /**
     * Handles all NotFound case of the API
     *
     * @param ex      The RuntimeException that is encountered
     * @param request the HttpServeletRequest, to display the path
     * @return ResponseEntity
     */
    @ExceptionHandler(value = {RecordNotFoundException.class, VersionNotFoundException.class})
    public ResponseEntity<Object> handleNotfound(RuntimeException ex, HttpServletRequest request) {
        APIExceptionResponse response = new APIExceptionResponse(ex.getMessage());
        response.setTimestamp(new Date());
        response.setStatus(HttpStatus.NOT_FOUND.value());
        response.setError(HttpStatus.NOT_FOUND.toString());
        response.setPath(request.getServletPath());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles forbidden 403 operations
     *
     * @param ex      The RuntimeException that is encountered
     * @param request the HttpServeletRequest, to display the path
     * @return ResponseEntity
     */
    @ExceptionHandler(value = {ForbiddenOperationException.class})
    public ResponseEntity<Object> handleForbidden(RuntimeException ex, HttpServletRequest request) {
        APIExceptionResponse response = new APIExceptionResponse(ex.getMessage());
        response.setTimestamp(new Date());
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setError(HttpStatus.FORBIDDEN.toString());
        response.setPath(request.getServletPath());
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(value = {SchemaNotSupportedException.class})
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
     *
     * @param ex handles MethodArgumentNotValidException
     * @param request The current request
     * @return APIExceptionResponse
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        APIExceptionResponse response = new APIExceptionResponse(ex.getMessage());
        response.setTimestamp(new Date());
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setError(HttpStatus.BAD_REQUEST.toString());
        response.setPath(request.getServletPath());

//        Map<String, String> errors = new HashMap<>();
//        ex.getBindingResult().getAllErrors().forEach((error) -> {
//            String fieldName = ((FieldError) error).getField();
//            String errorMessage = error.getDefaultMessage();
//            errors.put(fieldName, errorMessage);
//        });

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}
