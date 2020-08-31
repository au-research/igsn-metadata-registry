package au.edu.ardc.registry.oai.model;

import au.edu.ardc.registry.common.entity.Record;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRawValue;

@JsonPropertyOrder({"header", "metadata"})
public class RecordFragment {

    @JsonIgnore
    private Record record;

    @JsonProperty("header")
    private RecordHeaderFragment header;

    @JsonRawValue
    private String metadata;

    public RecordFragment(Record record, String metadata) {
        this.record = record;
        this.metadata = metadata.replaceAll("\\<\\?xml(.+?)\\?\\>", "").trim();
        this.header = new RecordHeaderFragment(record.getId().toString(), record.getModifiedAt().toString());
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Record getRecord() {
        return record;
    }

    public void setRecord(Record record) {
        this.record = record;
    }
}
