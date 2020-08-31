package au.edu.ardc.igsn.exception;

public class JSONValidationException extends RuntimeException{
    public JSONValidationException(String msg) {
        super(msg);
    }
}