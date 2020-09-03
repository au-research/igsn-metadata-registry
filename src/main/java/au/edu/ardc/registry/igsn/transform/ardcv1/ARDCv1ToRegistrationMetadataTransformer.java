package au.edu.ardc.registry.igsn.transform.ardcv1;

import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.common.transform.Transformer;
import au.edu.ardc.registry.common.transform.XSLTransformer;
import java.util.HashMap;
import java.util.Map;

public class ARDCv1ToRegistrationMetadataTransformer implements Transformer {

	private static final String path = "src/main/resources/xslt/ardc_v1_to_registration_metadata_v1.xsl";

	private static final String targetSchemaID = "igsn-reg-1.0";

	private Map<String, String> parameters = new HashMap<>();

	/**
	 * Transform a {@link Version} with schema ardcv1 to a {@link Version} with schema
	 * registration metadata
	 * @param version Input {@link Version} with schema ardcv1
	 * @return {@link Version} where igsn-reg-1.0 metadata and content are set to the
	 * transformed value
	 */
	public Version transform(Version version) {
		// the result is available via the StringWriter
		String resultDocument = XSLTransformer.transform(path, new String(version.getContent()), this.parameters);
		// build resultVersion
		if (resultDocument == null)
			return null;

		Version resultVersion = new Version();
		resultVersion.setSchema(targetSchemaID);
		resultVersion.setContent(resultDocument.getBytes());

		return resultVersion;
	}

	/**
	 * Adds or Updates parameters that is used by the XSLT Transform These parameters
	 * enable to create a richer registration metadata
	 * @param key String {registrantName| nameIdentifier| nameIdentifierScheme | eventType
	 * | timeStamp}
	 * @param value String the value of the given parameter
	 * @return
	 */
	public ARDCv1ToRegistrationMetadataTransformer setParam(String key, String value) {
		if (this.parameters.containsKey(key)) {
			this.parameters.replace(key, value);
		}
		else {
			this.parameters.put(key, value);
		}
		return this;
	}

	/**
	 * @return the parameters
	 */
	public Map<String, String> getParams() {
		return this.parameters;
	}

}
