package au.edu.ardc.registry.oai.exception;

public class NoSetHierarchy extends OAIException {

	public NoSetHierarchy() {
		super();
	}

	@Override
	public String getMessageID() {
		return "oai.error.no-set-hierarchy";
	}

	@Override
	public String getCode() {
		return OAIException.noSetHierarchyCode;
	}

}
