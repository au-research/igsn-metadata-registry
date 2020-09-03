package au.edu.ardc.registry.oai.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RecordHeaderFragment {

	public String identifier;

	@JsonProperty("datestamp")
	public String dateStamp;

	public RecordHeaderFragment(String identifier, String dateStamp) {
		this.identifier = identifier;
		this.dateStamp = dateStamp;
	}

}
