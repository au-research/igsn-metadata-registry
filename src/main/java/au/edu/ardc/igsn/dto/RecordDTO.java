package au.edu.ardc.igsn.dto;

import au.edu.ardc.igsn.entity.Record;
import com.fasterxml.jackson.annotation.JsonInclude;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.UUID;

public class RecordDTO {
    private UUID id;

    private boolean visible = true;

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

    private UUID dataCenterID;

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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getDataCenterID() {
        return dataCenterID;
    }

    public void setDataCenterID(UUID dataCenterID) {
        this.dataCenterID = dataCenterID;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
