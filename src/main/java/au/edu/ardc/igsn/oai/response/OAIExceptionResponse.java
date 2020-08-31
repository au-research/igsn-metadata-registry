package au.edu.ardc.igsn.oai.response;

import au.edu.ardc.igsn.oai.response.OAIResponse;

public class OAIExceptionResponse extends OAIResponse {
    private String error;

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
