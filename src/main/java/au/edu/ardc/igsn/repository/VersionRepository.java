package au.edu.ardc.igsn.repository;

import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.entity.Version;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.lang.annotation.Native;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VersionRepository extends JpaRepository<Version, String>, JpaSpecificationExecutor<Version> {

    Optional<Version> findById(UUID id);

    boolean existsById(UUID id);

    boolean existsByHash(String hash);

    boolean existsBySchemaAndHash(String schema, String hash);

    boolean existsBySchemaAndHashAndCurrent(String schema, String hash, boolean visible);

    Version findByRecordAndSchemaAndCurrentIsTrue(Record record, String schema);

    Version findFirstByRecordAndSchemaAndCurrentIsTrue(Record record, String schema);

    List<Version> findAllByRecordAndSchemaAndCurrentIsTrue(Record record, String schema);
}
