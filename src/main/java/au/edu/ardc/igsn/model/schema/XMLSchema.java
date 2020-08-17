package au.edu.ardc.igsn.model.schema;

import au.edu.ardc.igsn.model.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class XMLSchema extends Schema {

    private String namespace;
    private String schemaLocation;
    private String localSchemaLocation;

    public XMLSchema() {

    }

    @JsonProperty("xml")
    private void unpackXMLProperties(Map<String, String> xml) {
        this.namespace = xml.get("namespace");
        this.schemaLocation = xml.get("schemaLocation");
        this.localSchemaLocation = xml.get("localSchemaLocation");
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getSchemaLocation() {
        return schemaLocation;
    }

    public void setSchemaLocation(String schemaLocation) {
        this.schemaLocation = schemaLocation;
    }

    public String getLocalSchemaLocation() {
        return localSchemaLocation;
    }

    public void setLocalSchemaLocation(String localSchemaLocation) {
        this.localSchemaLocation = localSchemaLocation;
    }
}
