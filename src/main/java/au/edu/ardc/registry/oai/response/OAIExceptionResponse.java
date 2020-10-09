package au.edu.ardc.registry.oai.response;

import au.edu.ardc.registry.oai.model.ErrorFragment;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OAIExceptionResponse extends OAIResponse {

	@JsonProperty("error")
	private ErrorFragment error;

	public OAIExceptionResponse() {
		this.error = error;
	}

	public ErrorFragment getError() {
		return error;
	}

	public void setError(ErrorFragment error) {
		this.error = error;
	}

}
