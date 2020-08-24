package au.edu.ardc.igsn.oai.response;

import au.edu.ardc.igsn.oai.model.IdentifyFragment;
import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName("Identify")
public class OAIIdentifyResponse extends OAIResponse{
    private IdentifyFragment identify;

    public OAIIdentifyResponse(IdentifyFragment identify) {
        this.identify = identify;
    }

    public IdentifyFragment getIdentify() {
        return identify;
    }
}
