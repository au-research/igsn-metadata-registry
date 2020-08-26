package au.edu.ardc.igsn.repository;

import au.edu.ardc.igsn.entity.Identifier;
import au.edu.ardc.igsn.entity.Record;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdentifierRepository extends JpaRepository<Identifier, String>, JpaSpecificationExecutor<Identifier> {

    Optional<Identifier> findById(UUID id);

    boolean existsById(UUID id);

    Identifier findByValueAndType(String value, Identifier.Type type);

    Identifier findFirstByRecordAndType(Record record, Identifier.Type type);

    boolean existsByTypeAndValue(Identifier.Type type, String value);
}
