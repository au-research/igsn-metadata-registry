package au.edu.ardc.registry.igsn.provider;

import au.edu.ardc.registry.common.model.Schema;

public interface IdentifierProvider {
	String get(Schema Schema, String content);
}
