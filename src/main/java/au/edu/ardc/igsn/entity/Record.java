package au.edu.ardc.igsn.entity;


import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import javax.persistence.*;

import org.hibernate.annotations.GenericGenerator;

import java.util.List;

@Entity
@Table(name = "record")
@JsonIdentityInfo(
        generator = ObjectIdGenerators.UUIDGenerator.class,
        property = "id")

public class Record {
	
	@Id
    @GeneratedValue(generator="system-uuid")
	@GenericGenerator(name="system-uuid", strategy = "uuid")
    @Column(length = 125)
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



	public List<Version> getVersions() {
		return versions;
	}

	public void setVersions(List<Version> versions) {
		this.versions = versions;
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
