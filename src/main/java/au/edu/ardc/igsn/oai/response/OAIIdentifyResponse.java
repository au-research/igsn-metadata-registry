package au.edu.ardc.igsn.oai.response;

import au.edu.ardc.igsn.oai.model.IdentifyFragment;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

public class OAIIdentifyResponse extends OAIResponse{

    @JsonProperty("Identify")
    private IdentifyFragment identify;

    public OAIIdentifyResponse(IdentifyFragment identify) {
        this.identify = identify;
    }

    public IdentifyFragment getIdentify() {
        return identify;
    }
}
