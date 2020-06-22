package Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.entity.Registrant;

@Repository
public interface RecordRepository extends JpaRepository<Record , Long> {
	
	Optional<Record> findById(Long id);
	
	List<Record> GetbyRegiastrant(Registrant r);

}
