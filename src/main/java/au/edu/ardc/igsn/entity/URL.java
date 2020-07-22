package au.edu.ardc.igsn.entity;


import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "urls")

public class URL {

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(
			name = "UUID",
			strategy = "org.hibernate.id.UUIDGenerator"
	)
	@Column(columnDefinition = "BINARY(16)", updatable = false, nullable = false, unique = true)
	private UUID id;

    private String url;

	@Enumerated(EnumType.STRING)
	private URL.Status status;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

	@Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

	@Temporal(TemporalType.TIMESTAMP)
	private Date checkedAt;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "record_id", nullable = false)
	private Record record;

	public static enum Status {
		RESOLVABLE, UNKNOWN, BROKEN
	}

	/**
	 * Empty constructor
	 */
	public URL() {

	}

	public URL(UUID uuid) {
		this.id = uuid;
	}

	public UUID getId() {
		return id;
	}

    public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(java.util.Date created) {
		this.createdAt = created;
	}

	public Date getCheckedAt() {
		return checkedAt;
	}

	public void setCheckedAt(java.util.Date checkedAt) {
		this.checkedAt = checkedAt;
	}

	public Date getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(java.util.Date updatedAt) {
		this.updatedAt = updatedAt;
	}

	public Record getRecord() {
		return record;
	}

	public void setRecord(Record record) {
		this.record = record;
	}

}
