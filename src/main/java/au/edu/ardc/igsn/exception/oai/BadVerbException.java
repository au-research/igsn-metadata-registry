package au.edu.ardc.igsn.exception.oai;

public class BadVerbException extends RuntimeException{
    public BadVerbException() {
        super("Illegal OAI verb");
    }
}
