package au.edu.ardc.registry.common.entity;

import au.edu.ardc.registry.common.entity.converter.HashMapAttributeConverter;
import au.edu.ardc.registry.igsn.entity.IGSNEventType;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "requests")
public class Request {

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	@Column(columnDefinition = "BINARY(16)", updatable = false, nullable = false, unique = true)
	private UUID id;

	@SuppressWarnings("JpaAttributeTypeInspection")
	@Convert(converter = HashMapAttributeConverter.class)
	@Column(length=4096)
	private Map<String, String> attributes;

	@Enumerated(EnumType.STRING)
	private Status status;

	@Temporal(TemporalType.TIMESTAMP)
	private Date createdAt;

	@Temporal(TemporalType.TIMESTAMP)
	private Date updatedAt;

	@Column(columnDefinition = "BINARY(16)")
	private UUID createdBy;

	@Enumerated(EnumType.STRING)
	private IGSNEventType type;

	public Request() {
		this.attributes = new HashMap<>();
	}

    public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
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

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public Date getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}

	public UUID getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(UUID id) {
		this.createdBy = id;
	}

	public String getDataPath() {
		return attributes.get("dataPath");
	}

	public void setDataPath(String dataPath) {
		this.attributes.put("dataPath", dataPath);
	}

	public IGSNEventType getType() {
		return type;
	}

	public void setType(IGSNEventType type) {
		this.type = type;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}

	public enum Status {

		ACCEPTED, QUEUED, RUNNING, COMPLETED, FAILED

	}

}
