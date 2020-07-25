package au.edu.ardc.igsn.dto;

import au.edu.ardc.igsn.entity.Record;
import com.fasterxml.jackson.annotation.JsonInclude;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.UUID;

public class RecordDTO {
    private UUID id;

    @NotNull
    private Record.Status status;

    @NotNull
    private Date createdAt;

    @NotNull
    private Date modifiedAt;

    @NotNull
    private UUID creatorID;

    @NotNull
    private UUID modifierID;

    @NotNull
    private UUID allocationID;

    @NotNull
    private Record.OwnerType ownerType;

    @NotNull
    private UUID ownerID;

    public UUID getId() {
        return id;
    }

    public Date getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(Date modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public UUID getCreatorID() {
        return creatorID;
    }

    public void setCreatorID(UUID creatorID) {
        this.creatorID = creatorID;
    }

    public UUID getModifierID() {
        return modifierID;
    }

    public void setModifierID(UUID modifierID) {
        this.modifierID = modifierID;
    }

    public UUID getAllocationID() {
        return allocationID;
    }

    public void setAllocationID(UUID allocationID) {
        this.allocationID = allocationID;
    }

    public Record.OwnerType getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(Record.OwnerType ownerType) {
        this.ownerType = ownerType;
    }

    public UUID getOwnerID() {
        return ownerID;
    }

    public void setOwnerID(UUID ownerID) {
        this.ownerID = ownerID;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Record.Status getStatus() {
        return status;
    }

    public void setStatus(Record.Status status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
