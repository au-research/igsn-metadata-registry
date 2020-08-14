package au.edu.ardc.igsn.dto;

import au.edu.ardc.igsn.config.ApplicationProperties;
import org.jetbrains.annotations.PropertyKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

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
