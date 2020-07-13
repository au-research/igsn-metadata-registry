package au.edu.ardc.igsn.repository;

import au.edu.ardc.igsn.entity.Version;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VersionRepository extends JpaRepository<Version, String> {

    Optional<Version> findById(UUID id);

    boolean existsById(UUID id);

}
