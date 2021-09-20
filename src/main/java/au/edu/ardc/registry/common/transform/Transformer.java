package au.edu.ardc.registry.common.transform;

import au.edu.ardc.registry.common.entity.Version;

import java.util.Map;

public interface Transformer {

	Version transform(Version version);


	Transformer setParam(String key, String value);

	/**
	 * @return the parameters
	 */
	Map<String, String> getParams();

}
