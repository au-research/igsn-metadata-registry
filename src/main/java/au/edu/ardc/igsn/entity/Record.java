package au.edu.ardc.igsn.entity;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

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
    @Column(name = "id", length = 36, updatable = false, nullable = false, unique = true)
    private String id;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date deletedAt;

    @Column(length = 36)
    private String createdBy;

    @Column(length = 36)
    private String modifiedBy;

    @Column(length = 36)
    private String allocationID;

    @Column(length=36)
    private String dataCenterID;

    @Enumerated(EnumType.STRING)
    private OwnerType ownerType;

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
    public Record(String uuid) {
        this.id = uuid;
    }

    public String getId() {
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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

    public String getAllocationID() {
        return allocationID;
    }

    public void setAllocationID(String allocationID) {
        this.allocationID = allocationID;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public String getDataCenterID() {
        return dataCenterID;
    }

    public void setDataCenterID(String dataCenterID) {
        this.dataCenterID = dataCenterID;
    }

    public OwnerType getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(OwnerType ownerType) {
        this.ownerType = ownerType;
    }

    public static enum Status {
        PUBLISHED, DRAFT
    }

    public static enum OwnerType {
        User, DataCenter
    }

}
