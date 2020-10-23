package au.edu.ardc.registry.exception;

import io.swagger.v3.oas.annotations.Operation;

import java.util.Date;

public class VersionIsOlderThanCurrentException extends APIException {

    private final String identifierValue;

    private final Date currentDate;

    private final Date versionDate;

    public VersionIsOlderThanCurrentException(String identifierValue, Date currentDate, Date versionDate) {
        super();
        this.identifierValue = identifierValue;
        this.currentDate = currentDate;
        this.versionDate = versionDate;
    }

    @Override
    public String getMessage() {
        return String.format("Given version content is older than current version for " +
                        "Identifier %s current Date: %s, Incoming Date : %s", identifierValue,
                currentDate, versionDate);
    }

    @Override
    public String getMessageID() {
        return "api.error.version-already-exist";
    }

    @Override
    public String[] getArgs() {
        return new String[] { this.identifierValue, this.currentDate.toString(), this.versionDate.toString()};
    }

}