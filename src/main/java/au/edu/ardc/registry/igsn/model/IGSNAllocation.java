package au.edu.ardc.registry.igsn.model;

import au.edu.ardc.registry.common.model.Allocation;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class IGSNAllocation extends Allocation {

	private String mds_password;

	private String mds_username;

	private String mds_url;

	private String prefix;

	private String namespace;

	public IGSNAllocation(UUID id) {
		super(id);
	}

	/**
	 * Override the setAttributes for IGSN allocations and extract the MDS specific fields
	 * @param attributes a {@link Map} of attributes for transformation
	 */
	@Override
	public void setAttributes(Map<String, List<String>> attributes) {
		super.setAttributes(attributes);
		for (Map.Entry<String, List<String>> entry : attributes.entrySet()) {
			if (entry.getKey().equals("server_url"))
				this.mds_url = entry.getValue().get(0);
			if (entry.getKey().equals("password"))
				this.mds_password = entry.getValue().get(0);
			if (entry.getKey().equals("username"))
				this.mds_username = entry.getValue().get(0);
			if (entry.getKey().equals("prefix"))
				this.prefix = entry.getValue().get(0);
			if (entry.getKey().equals("namespace"))
				this.namespace = entry.getValue().get(0);
		}
	}

	public String getMds_url() {
		return this.mds_url;
	}

	public String getMds_password() {
		return this.mds_password;
	}

	public String getMds_username() {
		return this.mds_username;
	}

	public String getPrefix() {
		return this.prefix;
	}

	public String getNamespace() {
		return this.namespace;
	}

	public IGSNAllocation setMds_url(String md_url) {
		this.mds_url = md_url;
		return this;
	}

	public IGSNAllocation setMds_password(String mds_password) {
		this.mds_password = mds_password;
		return this;
	}

	public IGSNAllocation setMds_username(String mds_username) {
		this.mds_username = mds_username;
		return this;
	}

	public IGSNAllocation setPrefix(String prefix) {
		this.prefix = prefix;
		return this;
	}

	public IGSNAllocation setNamespace(String namespace) {
		this.namespace = namespace;
		return this;
	}

}
