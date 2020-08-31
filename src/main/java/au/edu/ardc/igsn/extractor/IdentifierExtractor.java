package au.edu.ardc.igsn.extractor;

import au.edu.ardc.igsn.entity.Identifier;
import au.edu.ardc.igsn.model.Schema;

public interface IdentifierExtractor {
	public String getIdentifier(Schema Schema, String content);
}
