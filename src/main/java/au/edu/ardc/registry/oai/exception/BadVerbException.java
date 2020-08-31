package au.edu.ardc.registry.oai.exception;

public class BadVerbException extends RuntimeException{
    public BadVerbException() {
        super("Illegal OAI verb");
    }
}
