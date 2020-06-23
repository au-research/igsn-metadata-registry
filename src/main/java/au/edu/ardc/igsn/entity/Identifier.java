package au.edu.ardc.igsn.entity;


import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import javax.persistence.*;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "identifier")
@JsonIdentityInfo(
        generator = ObjectIdGenerators.UUIDGenerator.class,
        property = "id")

public class Identifier {
	
	@Id
    @GeneratedValue(generator="system-uuid")
	@GenericGenerator(name="system-uuid", strategy = "uuid")
    @Column(length = 125)
    private String id;
    private String type;
    private String value;
    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date created;
    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date updated;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "record_id", nullable = false)
	private Record record;
    
    
	public String getType() {
		return type;
	}



	public void setType(String type) {
		this.type = type;
	}



	public String getValue() {
		return value;
	}



	public void setValue(String value) {
		this.value = value;
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
