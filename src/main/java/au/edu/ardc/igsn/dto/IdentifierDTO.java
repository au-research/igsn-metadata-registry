package au.edu.ardc.igsn.dto;

import au.edu.ardc.igsn.entity.Identifier;

import java.util.Date;
import java.util.UUID;

public class IdentifierDTO {
    private UUID id;
    private Identifier.Status status;
    private Identifier.Type type;
    private String value;
    private Date createdAt;
    private Date updatedAt;
    private UUID record;

    public IdentifierDTO() {

    }

    public IdentifierDTO(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Identifier.Status getStatus() {
        return status;
    }

    public void setStatus(Identifier.Status status) {
        this.status = status;
    }

    public Identifier.Type getType() {
        return type;
    }

    public void setType(Identifier.Type type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getRecord() {
        return record;
    }

    public void setRecord(UUID record) {
        this.record = record;
    }
}
