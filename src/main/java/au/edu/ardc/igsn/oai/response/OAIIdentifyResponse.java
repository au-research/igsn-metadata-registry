package au.edu.ardc.igsn.oai.response;

import au.edu.ardc.igsn.oai.model.Identify;
import com.fasterxml.jackson.annotation.JsonRootName;

import javax.xml.bind.annotation.XmlRootElement;

@JsonRootName("Identify")
public class OAIIdentifyResponse extends OAIResponse{
    private Identify identify;

    public OAIIdentifyResponse(Identify identify) {
        this.identify = identify;
    }

    public Identify getIdentify() {
        return identify;
    }
}
