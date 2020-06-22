package au.edu.ardc.igsn.entity;


import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "record")
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id")

public class Record {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String status;
    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date created;
    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date updated;
    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date deleted;
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrant_id", nullable = false)
	private Registrant registrant;
	
    @OneToMany(targetEntity = Version.class, mappedBy = "record")
    private List<Version> versions;

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

	public java.util.Date getDeleted() {
		return deleted;
	}

	public void setDeleted(java.util.Date deleted) {
		this.deleted = deleted;
	}

	public Registrant getRegistrant() {
		return registrant;
	}

	public void setRegistrant(Registrant registrant) {
		this.registrant = registrant;
	}


}
