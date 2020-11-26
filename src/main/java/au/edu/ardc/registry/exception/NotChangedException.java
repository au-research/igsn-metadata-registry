package au.edu.ardc.registry.exception;

/**
 * Exception when content or information is already set to the given one
 */
public class NotChangedException extends APIException {

    private final String targetObject;

    private final String targetField;

    private final String value;

    public NotChangedException(String targetObject, String targetField , String value) {
        super();
        this.targetField = targetField;
        this.targetObject = targetObject;
        this.value = value;
    }

    @Override
    public String getMessageID() {
        return "api.error.content_not_changed";
    }

    @Override
    public String getMessage() {
        return String.format("The %s's %s, already set to %s", targetObject, targetField , value);
    }

    @Override
    public String[] getArgs() {
        return new String[] { targetObject, targetField , value};
    }

}