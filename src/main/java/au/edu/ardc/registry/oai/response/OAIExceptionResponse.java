package au.edu.ardc.registry.oai.response;

public class OAIExceptionResponse extends OAIResponse {
    private String error;

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
