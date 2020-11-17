package au.edu.ardc.registry.common.transform;
import java.util.Map;

public interface RegistrationMetadataTransformer extends Transformer{


    RegistrationMetadataTransformer setParam(String key, String value);

    /**
     * @return the parameters
     */
    Map<String, String> getParams();

}
