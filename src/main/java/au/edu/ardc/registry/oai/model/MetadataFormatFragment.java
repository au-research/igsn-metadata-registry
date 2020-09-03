package au.edu.ardc.registry.oai.model;

import au.edu.ardc.registry.common.entity.Record;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRawValue;

@JsonPropertyOrder({ "metadataPrefix", "schema", "metadataNamespace" })
public class MetadataFormatFragment {

	@JsonProperty("metadataPrefix")
	private String metadataPrefix;

	@JsonProperty("schema")
	private String schema;

	@JsonProperty("metadataNamespace")
	private String metadataNamespace;

	@JsonRawValue
	private String metadata;

	public void metadataFormatFragment(String metadata) {
		this.metadataPrefix = metadataPrefix;
		this.schema = schema;
		this.metadataNamespace = metadataNamespace;
	}

	public String getMetadataPrefix() {
		return metadataPrefix;
	}

	public void setMetadataPrefix(String metadataPrefix) {
		this.metadataPrefix = metadataPrefix;
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public String getMetadataNamespace() {
		return metadataNamespace;
	}

	public void setMetadataNamespace(String metadataNamespace) {
		this.metadataNamespace = metadataNamespace;
	}

}
