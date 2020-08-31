package au.edu.ardc.registry.common.model;

import au.edu.ardc.registry.common.model.schema.JSONSchema;
import au.edu.ardc.registry.common.model.schema.XMLSchema;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
        @JsonSubTypes.Type(value = XMLSchema.class, name = "XMLSchema"),
        @JsonSubTypes.Type(value = JSONSchema.class, name = "JSONSchema")
})
public class Schema {

    private String id;
    private String name;
    private String description;
    private String type;

    public Schema() {
    }

    public Schema(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
