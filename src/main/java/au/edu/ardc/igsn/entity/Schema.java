package au.edu.ardc.igsn.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.util.List;

import javax.persistence.*;


// for reserved name use `` or []
@Entity
@Table(name = "`schema`")
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id")

public class Schema {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String uri;
    private String local_path;
    private boolean active;
    private boolean editable;
    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date created;
    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date updated;

    @OneToMany(targetEntity = Version.class, mappedBy = "schema")
    private List<Version> versions;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getLocal_path() {
		return local_path;
	}

	public void setLocal_path(String local_path) {
		this.local_path = local_path;
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

	public List<Version> getVersions() {
		return versions;
	}

	public void setVersion(List<Version> versions) {
		this.versions = versions;
	}
}
