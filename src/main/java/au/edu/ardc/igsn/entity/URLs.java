package au.edu.ardc.igsn.entity;


import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import javax.persistence.*;

@Entity
@Table(name = "urls")
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id")

public class URLs {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String url;
    private String status;
    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date created;
	@Temporal(TemporalType.TIMESTAMP)
    private java.util.Date updated;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "record_id", nullable = false)
	private Record record;
    
    public String getUrl() {
		return url;
	}


	public void setUrl(String url) {
		this.url = url;
	}


	public String getStatus() {
		return status;
	}


	public void setStatus(String status) {
		this.status = status;
	}


	public java.util.Date getCreated() {
		return created;
	}


	public void setCreated(java.util.Date created) {
		this.created = created;
	}


	public java.util.Date getUpdated() {
		return updated;
	}


	public void setUpdated(java.util.Date updated) {
		this.updated = updated;
	}


	public Record getRecord() {
		return record;
	}


	public void setRecord(Record record) {
		this.record = record;
	}

}
