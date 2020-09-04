package au.edu.ardc.registry.oai.response;

import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.oai.model.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class OAIListMetadataFormatsResponse extends OAIResponse {

	private ListMetadataFormatFragment listMetadataFormatFragment  ;

	private String metadataPrefix  ;
	private String schema  ;
	private String metadataNamespace  ;

	@JsonProperty("metadataFormat")
	public void setFormat(String metadataPrefix,String schema,String metadataNamespace){
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
