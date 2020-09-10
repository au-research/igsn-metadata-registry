package au.edu.ardc.registry.igsn.exception;

import au.edu.ardc.registry.exception.APIException;

public class IGSNNoValidContentForSchema extends APIException {

    private final String igsn;
    private final String schema;

    public IGSNNoValidContentForSchema(String igsn, String schema) {
        super();
        this.igsn = igsn;
        this.schema = schema;
    }

    @Override
    public String[] getArgs() {
        return new String[]{ igsn, schema };
    }

    @Override
    public String getMessageID() {
        return "igsn.error.no-valid-content-for-schema";
    }
}
