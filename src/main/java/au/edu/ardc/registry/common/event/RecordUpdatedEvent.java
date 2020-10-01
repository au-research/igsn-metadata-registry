package au.edu.ardc.registry.common.event;

import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.model.User;

public class RecordUpdatedEvent {

	private final Record record;

	private final User user;

	public RecordUpdatedEvent(Record record, User user) {
		this.record = record;
		this.user = user;
	}

	public Record getRecord() {
		return record;
	}

	public User getUser() {
		return user;
	}

}
