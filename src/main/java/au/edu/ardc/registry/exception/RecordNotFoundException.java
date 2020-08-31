package au.edu.ardc.registry.exception;

/**
 * Exception for when a Record is not found
 */
public class RecordNotFoundException extends RuntimeException{
    public RecordNotFoundException(String uuid) {
        super(String.format("Record with uuid:%s is not found within the registry", uuid));
    }
}
