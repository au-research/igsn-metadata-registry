package au.edu.ardc.registry.oai.response;

import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.oai.model.GetRecordFragment;
import au.edu.ardc.registry.oai.model.RecordFragment;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GetRecordResponse extends OAIResponse {

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
