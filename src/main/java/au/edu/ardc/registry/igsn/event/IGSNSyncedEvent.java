package au.edu.ardc.registry.igsn.event;

import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Request;

public class IGSNSyncedEvent {

	private Identifier identifier;

	private Request request;

	public IGSNSyncedEvent(Identifier identifier, Request request) {
		this.identifier = identifier;
		this.request = request;
	}

	public Identifier getIdentifier() {
		return identifier;
	}

	public void setIdentifier(Identifier identifier) {
		this.identifier = identifier;
	}

	public Request getRequest() {
		return request;
	}

	public void setRequest(Request request) {
		this.request = request;
	}

}
