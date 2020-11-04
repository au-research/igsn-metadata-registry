package au.edu.ardc.registry.exception;

/**
 * Exception for when an Embargo is not found
 */
public class EmbargoNotFoundException extends APIException {

    private final String id;

    public EmbargoNotFoundException(String uuid) {
        super();
        this.id = uuid;
    }

    @Override
    public String getMessageID() {
        return "api.error.embargo-not-found";
    }

    @Override
    public String[] getArgs() {
        return new String[] { this.id };
    }

}
