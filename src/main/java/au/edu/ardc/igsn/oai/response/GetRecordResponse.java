package au.edu.ardc.igsn.oai.response;

import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.oai.model.GetRecordFragment;
import au.edu.ardc.igsn.oai.model.RecordFragment;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GetRecordResponse extends OAIResponse{

    @JsonProperty("GetRecord")
    private GetRecordFragment recordFragment;

    private Record record;
    private String metadata;

    public GetRecordResponse(Record record, String metadata) {
        this.recordFragment = new GetRecordFragment();
        RecordFragment recordFragment = new RecordFragment(record, metadata);
        this.recordFragment.setRecord(recordFragment);
    }

}
