package au.edu.ardc.igsn.entity;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "records")
public class Record {

    // todo soft delete

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(columnDefinition = "BINARY(16)", updatable = false, nullable = false, unique = true)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date deletedAt;

    @Column(columnDefinition = "BINARY(16)")
    private UUID creatorID;

    @Column(columnDefinition = "BINARY(16)")
    private UUID modifierID;

    @Column(columnDefinition = "BINARY(16)")
    private UUID allocationID;

    @Column(columnDefinition = "BINARY(16)")
    private UUID dataCenterID;

    @Enumerated(EnumType.STRING)
    private OwnerType ownerType;

    @Column(columnDefinition = "BINARY(16)")
    private UUID ownerID;

    @OneToMany(targetEntity = Version.class, mappedBy = "record")
    private List<Version> versions;

    /**
     * Empty constructor
     */
    public Record() {

    }

    /**
     * Constructor with uuid
     */
    public Record(UUID uuid) {
        this.id = uuid;
    }

    public UUID getId() {
        return id;
    }

    public List<Version> getVersions() {
        return versions;
    }

    public void setVersions(List<Version> versions) {
        this.versions = versions;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public UUID getCreatorID() {
        return creatorID;
    }

    public void setCreatorID(UUID createdBy) {
        this.creatorID = createdBy;
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

    public UUID getAllocationID() {
        return allocationID;
    }

    public void setAllocationID(UUID allocationID) {
        this.allocationID = allocationID;
    }

    public UUID getModifierID() {
        return modifierID;
    }

    public void setModifierID(UUID modifiedBy) {
        this.modifierID = modifiedBy;
    }

    public UUID getDataCenterID() {
        return dataCenterID;
    }

    public void setDataCenterID(UUID dataCenterID) {
        this.dataCenterID = dataCenterID;
    }

    public OwnerType getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(OwnerType ownerType) {
        this.ownerType = ownerType;
    }

    public UUID getOwnerID() {
        return ownerID;
    }

    public void setOwnerID(UUID ownerID) {
        this.ownerID = ownerID;
    }

    public static enum Status {
        PUBLISHED, DRAFT
    }

    public static enum OwnerType {
        User, DataCenter
    }

}
