package au.edu.ardc.igsn.entity;


import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Table(name = "versions")
public class Version {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(length = 125)
    private String id;

    private String status;

    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date embargo;

    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date created;

    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date updated;

    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date ended;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "record_id", nullable = false)
    private Record record;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schema_id", nullable = false)
    private SchemaEntity schema;


    public Record getRecord() {
        return record;
    }

    public void setRecord(Record record) {
        this.record = record;
    }

    public SchemaEntity getSchema() {
        return schema;
    }

    public void setSchema(SchemaEntity schema) {
        this.schema = schema;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public java.util.Date getEmbargo() {
        return embargo;
    }

    public void setEmbargo(java.util.Date embargo) {
        this.embargo = embargo;
    }

    public java.util.Date getCreated() {
        return created;
    }

    public void setCreated(java.util.Date created) {
        this.created = created;
    }

    public java.util.Date getUpdated() {
        return updated;
    }

    public void setUpdated(java.util.Date updated) {
        this.updated = updated;
    }

    public java.util.Date getEnded() {
        return ended;
    }

    public void setEnded(java.util.Date ended) {
        this.ended = ended;
    }


}
