package au.edu.ardc.registry.igsn.dto;

import au.edu.ardc.registry.common.dto.RecordDTO;

public class IGSNRecordDTO extends RecordDTO {

	private String igsn;

	private String portalUrl;

	public String getIgsn() {
		return igsn;
	}

	public void setIgsn(String igsn) {
		this.igsn = igsn;
	}

	public String getPortalUrl() {
		return portalUrl;
	}

	public void setPortalUrl(String portalUrl) {
		this.portalUrl = portalUrl;
	}

}
