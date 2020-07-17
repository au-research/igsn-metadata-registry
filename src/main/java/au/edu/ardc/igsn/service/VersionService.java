package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.entity.Version;
import au.edu.ardc.igsn.repository.VersionRepository;
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

    /**
     * Permanently delete the version
     *
     * @param id the uuid of the Version
     */
    public void delete(String id) {
        repository.deleteById(id);
    }
}
