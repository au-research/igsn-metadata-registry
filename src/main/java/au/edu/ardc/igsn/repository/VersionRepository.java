package au.edu.ardc.igsn.repository;

import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.entity.Version;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VersionRepository extends JpaRepository<Version, String> {

    Optional<Version> findById(UUID id);

    boolean existsById(UUID id);

    boolean existsByHash(String hash);

    boolean existsBySchemaAndHash(String schema, String hash);

    boolean existsBySchemaAndHashAndCurrent(String schema, String hash, boolean visible);

    Page<Version> findAllByRecord(Record record, Pageable pageable);
}
