package au.edu.ardc.registry.common.provider;

import au.edu.ardc.registry.common.model.Schema;

public interface IdentifierProvider {
	String get(String content);
}
