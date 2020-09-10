package au.edu.ardc.registry.igsn.config;

import au.edu.ardc.registry.exception.APIException;
import au.edu.ardc.registry.exception.APIExceptionResponse;
import au.edu.ardc.registry.exception.RecordNotFoundException;
import au.edu.ardc.registry.exception.VersionNotFoundException;
import au.edu.ardc.registry.igsn.exception.IGSNNoValidContentForSchema;
import au.edu.ardc.registry.igsn.exception.IGSNNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

@RestControllerAdvice
public class IGSNControllerAdvice {

    @Autowired
    MessageSource messageSource;

    @ExceptionHandler(value = { IGSNNotFoundException.class, IGSNNoValidContentForSchema.class })
    public ResponseEntity<Object> handleNotfound(APIException ex, HttpServletRequest request, Locale locale) {
        String message = messageSource.getMessage(ex.getMessageID(), ex.getArgs(), locale);
        APIExceptionResponse response = new APIExceptionResponse(message, HttpStatus.NOT_FOUND, request);

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
}
