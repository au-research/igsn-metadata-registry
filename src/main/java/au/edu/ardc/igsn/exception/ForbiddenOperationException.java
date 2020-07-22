package au.edu.ardc.igsn.exception;

public class ForbiddenOperationException extends RuntimeException{
    public ForbiddenOperationException(String msg) {
        super(msg);
    }
}
