package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.entity.Version;
import au.edu.ardc.igsn.repository.VersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
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
        repository.save(version);

        return version;
    }

    /**
     * Find a version by id
     *
     * @param id String representation of an uuid
     * @return the version if it exists, null if not
     */
    public Version findById(String id) {
        Optional<Version> opt = repository.findById(UUID.fromString(id));

        return opt.orElse(null);
    }

    /**
     * Tell if a version exists by id
     *
     * @param id String uuid
     * @return if the uuid correlate to an existing version
     */
    public boolean exists(String id) {
        return repository.existsById(UUID.fromString(id));
    }
}
