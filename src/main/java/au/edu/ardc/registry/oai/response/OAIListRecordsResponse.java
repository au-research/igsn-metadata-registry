package au.edu.ardc.registry.oai.response;

import au.edu.ardc.registry.oai.model.ListRecordsFragment;
import au.edu.ardc.registry.oai.model.ResumptionTokenFragment;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OAIListRecordsResponse extends OAIResponse {

	@JsonProperty("ListRecords")
	private ListRecordsFragment listRecordsFragment;

	public void setRecordsFragment(ListRecordsFragment listRecordsFragment) {
		this.listRecordsFragment = listRecordsFragment;
	}

	public ListRecordsFragment getListRecordsFragment() {
		return this.listRecordsFragment;
	}

}
