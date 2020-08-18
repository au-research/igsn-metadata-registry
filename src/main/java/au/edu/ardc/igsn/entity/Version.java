package au.edu.ardc.igsn.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "versions", indexes = {
        @Index(name = "idx_schema_current", columnList = "schema_id,current")
})
public class Version {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(columnDefinition = "BINARY(16)", updatable = false, nullable = false, unique = true)
    private UUID id;

    private boolean current = false;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date endedAt;

    @Column(columnDefinition = "BINARY(16)")
    private UUID creatorID;

    @Column(columnDefinition = "BINARY(16)")
    private UUID endedBy;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id", nullable = false)
    private Record record;

    @Column(name = "schema_id")
    private String schema;

    @JsonIgnore
    @Lob
    @Column(columnDefinition = "BLOB")
    private byte[] content;

    private String hash;

    /**
     * Empty constructor
     */
    public Version() {

    }

    /**
     * Constructor with uuid
     * Keep in mind the record once persist will have the uuid generated by Hibernate
     *
     * @param uuid the UUID to instantiate this record for
     */
    public Version(UUID uuid) {
        this.id = uuid;
    }

    @JsonIgnore
    public Record getRecord() {
        return record;
    }

    @JsonProperty
    public void setRecord(Record record) {
        this.record = record;
    }

    public UUID getId() {
        return id;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Date endedAt) {
        this.endedAt = endedAt;
    }

    public UUID getCreatorID() {
        return creatorID;
    }

    public void setCreatorID(UUID creatorID) {
        this.creatorID = creatorID;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public UUID getEndedBy() {
        return endedBy;
    }

    public void setEndedBy(UUID endedBy) {
        this.endedBy = endedBy;
    }
}
