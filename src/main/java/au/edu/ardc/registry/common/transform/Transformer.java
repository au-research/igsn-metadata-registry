package au.edu.ardc.registry.common.transform;

import au.edu.ardc.registry.common.entity.Version;

public interface Transformer {

	Version transform(Version version);

}
