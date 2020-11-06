package au.edu.ardc.registry.exception;

public class MDSClientException extends APIException {

    private final String msg;

    public MDSClientException(String msg) {
        super();
        this.msg = msg;
    }

    @Override
    public String getMessageID() {
        return "api.error.mds_client_exception";
    }

    @Override
    public String[] getArgs() {
        return new String[] { this.msg };
    }

    @Override
    public String getMessage() {
        return this.msg;
    }
}