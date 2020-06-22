package au.edu.ardc.igsn.entity;


import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "version")
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id")

public class Version {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String status;
    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date embargo;
    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date created;
    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date updated;
    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date ended;
    
    
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "record_id", nullable = false)
	private Record record;
	
    
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public java.util.Date getEmbargo() {
		return embargo;
	}
	public void setEmbargo(java.util.Date embargo) {
		this.embargo = embargo;
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
	public java.util.Date getEnded() {
		return ended;
	}
	public void setEnded(java.util.Date ended) {
		this.ended = ended;
	}
    


}
