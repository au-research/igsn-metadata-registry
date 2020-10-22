package au.edu.ardc.registry.common.repository;

import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.URL;
import au.edu.ardc.registry.common.entity.Version;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface URLRepository extends JpaRepository<URL, String> {

	Optional<URL> findById(UUID id);

	boolean existsById(UUID id);

	Optional<URL> findByRecord(Record record);

}
