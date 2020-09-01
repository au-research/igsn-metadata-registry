package au.edu.ardc.registry.igsn.transform.ardcv1;

import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.common.transform.Transformer;
import au.edu.ardc.registry.common.transform.XSLTransformer;
import org.json.JSONObject;

public class ARDCv1ToJSONLDTransformer implements Transformer {

    private static final String path = "src/main/resources/xslt/ardc_v1_to_jsonld.xsl";
    private static final String targetSchemaID = "ardc-igsn-desc-1.0-jsonld";

    /**
     * Transform a {@link Version} with schema ardcv1 to a {@link Version} with schema ardcjsonld
     *
     * @param version Input {@link Version} with schema ardcv1
     * @return {@link Version} where the schema and content are set to the transformed value
     */
    public Version transform(Version version) {
        // the result is available via the StringWriter
        String resultDocument = XSLTransformer.transform(path, new String(version.getContent()), null);
        // todo check resultDocument for null | try catch exception

        // prettify result json
        JSONObject json = new JSONObject(resultDocument);
        String formattedJSONString = json.toString(2);

        // build resultVersion
        Version resultVersion = new Version();
        resultVersion.setSchema(targetSchemaID);
        resultVersion.setContent(formattedJSONString.getBytes());

        return resultVersion;
    }

}
