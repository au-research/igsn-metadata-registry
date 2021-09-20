package au.edu.ardc.registry.oai.response;

import au.edu.ardc.registry.oai.model.*;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OAIListMetadataFormatsResponse<metadataFormat> extends OAIResponse {

	@JsonProperty("ListMetadataFormats")
	private ListMetadataFormatsFragment listMetadataFormatsFragment;

	public void setListMetadataFormatsFragment(ListMetadataFormatsFragment listMetadataFormatsFragment) {
		this.listMetadataFormatsFragment = listMetadataFormatsFragment;
	}

	public ListMetadataFormatsFragment getListMetadataFormatsFragment() {
		return this.listMetadataFormatsFragment;
	}

}
