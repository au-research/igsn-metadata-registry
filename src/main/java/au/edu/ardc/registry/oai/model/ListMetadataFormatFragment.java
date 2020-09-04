package au.edu.ardc.registry.oai.model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ListMetadataFormatFragment {

	private ListMetadataFormatFragment listMetadataFormat;

	@JsonProperty("metadataFormat")
	private String metadataFormats;

	public ListMetadataFormatFragment getListMetadataFormat() {
		return listMetadataFormat;
	}

	public void setListMetadataFormat(ListMetadataFormatFragment listMetadataFormat) {
		this.listMetadataFormat = listMetadataFormat;
	}

}
