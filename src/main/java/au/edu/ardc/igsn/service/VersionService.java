package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.dto.VersionDTO;
import au.edu.ardc.igsn.dto.VersionMapper;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.entity.Version;
import au.edu.ardc.igsn.exception.ForbiddenOperationException;
import au.edu.ardc.igsn.exception.RecordNotFoundException;
import au.edu.ardc.igsn.exception.SchemaNotSupportedException;
import au.edu.ardc.igsn.exception.VersionNotFoundException;
import au.edu.ardc.igsn.model.Allocation;
import au.edu.ardc.igsn.model.Scope;
import au.edu.ardc.igsn.model.User;
import au.edu.ardc.igsn.repository.VersionRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
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

    /**
     * End the life of a version
     *
     * @param version the version to end
     * @return the ended version
     */
    public Version end(Version version) {
        version.setEndedAt(new Date());
        version.setCurrent(false);

        // todo endBy currently logged in user
        repository.save(version);
        return version;
    }

    /**
     * Find a version by id
     *
     * @param id the uuid of the Version
     * @return the version if it exists, null if not
     */
    public Version findById(String id) {
        Optional<Version> opt = repository.findById(UUID.fromString(id));

        return opt.orElse(null);
    }

    /**
     * Retrieve all owned versions
     * Owned versions are the versions that which records the user have access to
     *
     * todo accept User UUID as a parameter
     * todo update findOwned at the repository level
     * @return a list of Versions that is owned by the user
     */
    public List<Version> findOwned() {
        return repository.findAll();
    }

    /**
     * Tell if a version exists by id
     *
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
            throw new ForbiddenOperationException("The repository already contains a current version with this data");
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
            version.setCreatorID(dto.getCreatorID() != null ? UUID.fromString(dto.getCreatorID()) : version.getCreatorID());
            version.setCurrent(dto.isCurrent());
        }

        // todo if this version is the current, end all other version of the same schema

        version = repository.save(version);

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

    public String getHash(Version version) {
        return DigestUtils.sha1Hex(version.getContent());
    }
}
