package au.edu.ardc.registry.oai.exception;

public class NoSetHierarchyException extends OAIException {

	public NoSetHierarchyException() {
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
