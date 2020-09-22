package au.edu.ardc.registry.common.service;

import au.edu.ardc.registry.common.dto.VersionDTO;
import au.edu.ardc.registry.common.dto.mapper.VersionMapper;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.common.event.RecordUpdatedEvent;
import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.exception.*;
import au.edu.ardc.registry.common.model.Allocation;
import au.edu.ardc.registry.common.model.Scope;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.repository.VersionRepository;
import au.edu.ardc.registry.common.repository.specs.SearchCriteria;
import au.edu.ardc.registry.common.repository.specs.SearchOperation;
import au.edu.ardc.registry.common.repository.specs.VersionSpecification;
import com.google.common.base.Converter;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Primary
@Service("VersionService")
public class VersionService {

	@Autowired
	private VersionRepository repository;

	@Autowired
	private VersionMapper mapper;

	@Autowired
	private SchemaService schemaService;

	@Autowired
	private RecordService recordService;

	@Autowired
	private ValidationService validationService;

	@Autowired
	ApplicationEventPublisher publisher;

	/**
	 * End the life of a {@link Version} The registry supports soft deleting of a
	 * {@link Version} so it's recommended to use this method to end the effective use of
	 * that {@link Version}
	 * @param version the {@link Version} to end
	 * @param user the {@link User} to end it with
	 * @return the ended {@link Version}
	 */
	public Version end(Version version, User user) {
		version.setEndedAt(new Date());
		version.setCurrent(false);
		version.setEndedBy(user.getId());

		repository.save(version);
		return version;
	}

	/**
	 * Find a {@link Version} by id
	 * @param id the String uuid of the {@link Version}
	 * @return the {@link VersionDTO} if it exists, {@code null} if not
	 */
	public Version findById(String id) {
		Optional<Version> opt = repository.findById(UUID.fromString(id));

		return opt.orElse(null);
	}

	/**
	 * Find a Public {@link Version} by id Public {@link Version} are Versions that
	 * belongs to a {@link Record} that is visible
	 * @param id the String uuid of the {@link Version}
	 * @return {@link VersionDTO}
	 */
	public VersionDTO findPublicById(String id) {
		Version version = findById(id);
		if (version == null || !version.getRecord().isVisible()) {
			throw new VersionNotFoundException(id);
		}
		return mapper.convertToDTO(version);
	}

	public VersionMapper getMapper() {
		return mapper;
	}

	/**
	 * Search for {@link Version} with {@link VersionSpecification}
	 * @param specs {@link VersionSpecification} that includes {@link SearchCriteria}
	 * @param pageable {@link Pageable}
	 * @return page {@link VersionDTO}
	 */
	public Page<VersionDTO> search(VersionSpecification specs, Pageable pageable) {
		Page<Version> versions = repository.findAll(specs, pageable);
		return versions.map(getDTOConverter());
	}

	/**
	 * Search for {@link Version} with {@link VersionSpecification}
	 * @param specs {@link VersionSpecification} that includes {@link SearchCriteria}
	 * @param pageable {@link Pageable}
	 * @return page {@link VersionDTO}
	 */
	public Page<Version> searchVersions(VersionSpecification specs, Pageable pageable) {
		Page<Version> versions = repository.findAll(specs, pageable);
		return versions;
	}

	/**
	 * Search for all {@link Version} that belongs to a particular {@link Record} uses the
	 * internal {@link #search} method to provide the formatting of the result
	 * @param record {@link Record}
	 * @param pageable {@link Pageable}
	 * @return a @{@link Page} of {@link VersionDTO}
	 */
	public Page<VersionDTO> findAllVersionsForRecord(Record record, Pageable pageable) {
		VersionSpecification specs = new VersionSpecification();
		specs.add(new SearchCriteria("record", record, SearchOperation.EQUAL));
		return search(specs, pageable);
	}

	/**
	 * Search for all public {@link Version} that has a particular {@link Schema} uses the
	 * internal {@link #searchVersions} method to provide the formatting of the result
	 * @param schema {@link String}
	 * @param pageable {@link Pageable}
	 * @return a @{@link Page} of {@link Version}
	 */
	public Page<Version> findAllCurrentVersionsOfSchema(String schema, Pageable pageable) {
		VersionSpecification specs = new VersionSpecification();
		specs.add(new SearchCriteria("schema", schema, SearchOperation.EQUAL));
		specs.add(new SearchCriteria("visible", true, SearchOperation.RECORD_EQUAL));
		Page<Version> versions = searchVersions(specs, pageable);
		return versions;
	}

	/**
	 * Return a single {@link Version} for a {@link Record} given the Schema string
	 * @param record {@link Record}
	 * @param schema Schema id of a {@link au.edu.ardc.registry.common.model.Schema}
	 * @return Version {@link Version}
	 */
	public Version findVersionForRecord(Record record, String schema) {
		return repository.findFirstByRecordAndSchemaAndCurrentIsTrue(record, schema);
	}

	/**
	 * Useful {@link Converter} for use with converting between {@link Version} and
	 * {@link VersionDTO} Uses for {@link #search} method to help provide results in DTO
	 * versions
	 * @return Converter {@link Converter}
	 */
	public Converter<Version, VersionDTO> getDTOConverter() {
		return new Converter<Version, VersionDTO>() {
			@Override
			protected VersionDTO doForward(Version version) {
				return mapper.convertToDTO(version);
			}

			@Override
			protected Version doBackward(VersionDTO versionDTO) {
				return mapper.convertToEntity(versionDTO);
			}
		};
	}

	/**
	 * Retrieve all owned versions Owned versions are the versions that which records the
	 * user have access to
	 * <p>
	 * todo accept User UUID as a parameter todo update findOwned at the repository level
	 * @return a list of Versions that is owned by the user
	 */
	public List<Version> findOwned() {
		return repository.findAll();
	}

	/**
	 * Tell if a version exists by id
	 * @param id the uuid of the Version
	 * @return if the uuid correlate to an existing version
	 */
	public boolean exists(String id) {
		return repository.existsById(UUID.fromString(id));
	}

	// create
	public Version create(Version newVersion) {
		return repository.save(newVersion);
	}

	public Version save(Version version) {
		if (version.getCreatedAt() == null) {
			version.setCreatedAt(new Date());
		}
		return repository.saveAndFlush(version);
	}

	public VersionDTO create(VersionDTO dto, User user) {
		// versionDTO should already be @Valid from controller
		Version version = mapper.convertToEntity(dto);

		// validate schema
		if (!schemaService.supportsSchema(version.getSchema())) {
			throw new SchemaNotSupportedException(version.getSchema());
		}

		if (dto.getRecord() == null || !recordService.exists(dto.getRecord())) {
			throw new RecordNotFoundException(dto.getRecord());
		}
		Record record = recordService.findById(dto.getRecord());

		if (!validationService.validateRecordOwnership(record, user)) {
			throw new ForbiddenOperationException("You don't own this record");
		}

		// there's already a version with this data, schema and is current
		String hash = getHash(version);
		if (repository.existsBySchemaAndHashAndCurrent(version.getSchema(), hash, true)) {
			throw new VersionContentAlreadyExisted(version.getSchema(), hash);
		}

		// todo validate version content -> schema validation

		// default
		version.setCreatedAt(new Date());
		version.setCreatorID(user.getId());
		version.setCurrent(true);
		version.setHash(getHash(version));

		// allow igsn:import scope to overwrite data
		Allocation allocation = new Allocation(record.getAllocationID());
		if (validationService.validateAllocationScope(allocation, user, Scope.IMPORT)) {
			version.setCreatedAt(dto.getCreatedAt() != null ? dto.getCreatedAt() : version.getCreatedAt());
			version.setCreatorID(
					dto.getCreatorID() != null ? UUID.fromString(dto.getCreatorID()) : version.getCreatorID());
			version.setCurrent(dto.isCurrent());
		}

		// if this version is the current, end all previous versions of the same schema
		if (version.isCurrent()) {
			List<Version> previousVersions = repository.findAllByRecordAndSchemaAndCurrentIsTrue(version.getRecord(),
					version.getSchema());
			for (Version previousVersion : previousVersions) {
				this.end(previousVersion, user);
			}
		}

		version = repository.save(version);

		// RecordUpdatedEvent
		publisher.publishEvent(new RecordUpdatedEvent(version.getRecord(), user));

		return mapper.convertToDTO(version);
	}

	public boolean delete(String id, User user) {
		if (!repository.existsById(UUID.fromString(id))) {
			throw new VersionNotFoundException(id);
		}
		Version version = findById(id);
		Record record = version.getRecord();
		if (!validationService.validateRecordOwnership(record, user)) {
			throw new ForbiddenOperationException("You don't have access to delete this version");
		}

		repository.deleteById(id);
		return true;
	}

	public static String getHash(Version version) {
		return DigestUtils.sha1Hex(version.getContent());
	}

	public static String getHash(String content) {
		return DigestUtils.sha1Hex(content);
	}

}
