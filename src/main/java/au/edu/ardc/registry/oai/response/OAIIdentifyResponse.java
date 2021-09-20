package au.edu.ardc.registry.oai.response;

import au.edu.ardc.registry.oai.model.IdentifyFragment;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OAIIdentifyResponse extends OAIResponse {

	@JsonProperty("Identify")
	private IdentifyFragment identify;

	public OAIIdentifyResponse(IdentifyFragment identify) {
		this.identify = identify;
	}

	public IdentifyFragment getIdentify() {
		return identify;
	}

}
