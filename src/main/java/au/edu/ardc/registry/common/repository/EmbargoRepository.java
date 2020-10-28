package au.edu.ardc.registry.common.repository;

import au.edu.ardc.registry.common.entity.Embargo;
import au.edu.ardc.registry.common.entity.Record;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmbargoRepository extends JpaRepository<Embargo, String> {

	Optional<Embargo> findById(UUID id);

	Optional<Embargo> findByRecord(Record record);

	@Query(value = "SELECT e FROM Embargo e WHERE e.embargoEnd <= ?1")
	List<Embargo> findAllByEmbargoEndLessThanEqual(Date date);

}
