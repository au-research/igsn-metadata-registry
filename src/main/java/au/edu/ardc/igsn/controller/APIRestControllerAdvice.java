package au.edu.ardc.igsn.controller;

import au.edu.ardc.igsn.exception.APIExceptionResponse;
import au.edu.ardc.igsn.exception.RecordNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

@RestControllerAdvice
public class APIRestControllerAdvice {

    /**
     * Handles all NotFound case of the API
     *
     * @param ex      The RuntimeException that is encountered
     * @param request the HttpServeletRequest, to display the path
     * @return ResponseEntity
     */
    @ExceptionHandler(value = {RecordNotFoundException.class})
    public ResponseEntity<Object> handleNotfound(RuntimeException ex, HttpServletRequest request) {
        APIExceptionResponse response = new APIExceptionResponse(ex.getMessage());
        response.setTimestamp(new Date());
        response.setStatus(HttpStatus.NOT_FOUND.value());
        response.setError(HttpStatus.NOT_FOUND.toString());
        response.setPath(request.getServletPath());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
}
