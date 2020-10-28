package au.edu.ardc.registry.common.provider;

import au.edu.ardc.registry.common.model.Schema;

import java.util.List;

public interface IdentifierProvider {

	String get(String content);

	String get(String content, int position);

	List<String> getAll(String content);

}
