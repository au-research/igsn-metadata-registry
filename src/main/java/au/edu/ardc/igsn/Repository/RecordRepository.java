package au.edu.ardc.igsn.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import au.edu.ardc.igsn.entity.Record;


@Repository
public interface RecordRepository extends JpaRepository<Record , String> {
	Optional<Record> findById(String id);
}
