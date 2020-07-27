package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.dto.VersionDTO;
import au.edu.ardc.igsn.dto.VersionMapper;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.entity.Version;
import au.edu.ardc.igsn.exception.RecordNotFoundException;
import au.edu.ardc.igsn.exception.SchemaNotSupportedException;
import au.edu.ardc.igsn.model.User;
import au.edu.ardc.igsn.repository.VersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

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

    /**
     * End the life of a version
     *
     * @param version the version to end
     * @return the ended version
     */
    public Version end(Version version) {
        version.setEndedAt(new Date());
        version.setStatus(Version.Status.SUPERSEDED);
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

        // todo validate user ownership
        // todo validate version content -> schema validation

        // default
        version.setCreatedAt(new Date());
        version.setCreatorID(user.getId());

        // todo allow import scope to overwrite data

        version = repository.save(version);

        return mapper.convertToDTO(version);
    }

    /**
     * Permanently delete the version
     *
     * @param id the uuid of the Version
     */
    public void delete(String id) {
        repository.deleteById(id);
    }
}
