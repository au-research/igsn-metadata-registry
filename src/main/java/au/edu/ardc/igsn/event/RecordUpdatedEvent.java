package au.edu.ardc.igsn.event;

import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.model.User;

public class RecordUpdatedEvent {
    private Record record;
    private User user;

    public RecordUpdatedEvent(Record record, User user) {
        this.record = record;
        this.user = user;
    }

    public Record getRecord() {
        return record;
    }

    public void setRecord(Record record) {
        this.record = record;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
