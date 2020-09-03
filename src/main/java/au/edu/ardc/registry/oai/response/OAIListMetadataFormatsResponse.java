package au.edu.ardc.registry.oai.response;

import au.edu.ardc.registry.oai.model.MetadataFormatFragment;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OAIListMetadataFormatsResponse extends OAIResponse {

	@JsonProperty("ListMetadataFormats")

	private MetadataFormatFragment metadataFormatFragment;

	public MetadataFormatFragment getFormat() {
		return metadataFormatFragment;
	}

	public void setFormat(MetadataFormatFragment value) {
		this.metadataFormatFragment = value;
	}

}
