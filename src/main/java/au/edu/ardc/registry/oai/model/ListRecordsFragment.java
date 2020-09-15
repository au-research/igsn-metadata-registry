package au.edu.ardc.registry.oai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

import java.util.ArrayList;
import java.util.List;

public class ListRecordsFragment {

	@JsonProperty("record")
	@JacksonXmlElementWrapper(useWrapping = false)

	private List<RecordFragment> listRecords = new ArrayList<RecordFragment>();

	public void setListRecords(RecordFragment recordFragment) {
		listRecords.add(recordFragment);
	}

}
