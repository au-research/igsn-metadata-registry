package au.edu.ardc.igsn.dto;

import au.edu.ardc.igsn.entity.Version;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Base64;
import java.util.Date;

public class VersionDTO {
    private String id;
    private String schema;
    private Version.Status status;
    private Date createdAt;
    private String creatorID;
    private String record;
    private String content;

    public String getRecord() {
        return record;
    }

    public void setRecord(String record) {
        this.record = record;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getContent() {
        return this.content;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatorID() {
        return creatorID;
    }

    public void setCreatorID(String creatorID) {
        this.creatorID = creatorID;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
