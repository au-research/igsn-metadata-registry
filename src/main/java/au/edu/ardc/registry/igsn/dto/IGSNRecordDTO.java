package au.edu.ardc.registry.igsn.dto;

import au.edu.ardc.registry.common.dto.IdentifierDTO;
import au.edu.ardc.registry.common.dto.RecordDTO;
import au.edu.ardc.registry.common.entity.Identifier;

import java.util.Date;

public class IGSNRecordDTO extends RecordDTO {

	private IdentifierDTO igsn;

	private String portalUrl;

	private Date embargoDate;

	private String status;

	public IdentifierDTO getIgsn() {
		return igsn;
	}

	public void setIgsn(IdentifierDTO igsn) {
		this.igsn = igsn;
	}

	public String getPortalUrl() {
		return portalUrl;
	}

	public void setPortalUrl(String portalUrl) {
		this.portalUrl = portalUrl;
	}

	public Date getEmbargoDate() {
		return embargoDate;
	}

	public void setEmbargoDate(Date embargoDate) {
		this.embargoDate = embargoDate;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
