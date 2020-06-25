package au.edu.ardc.igsn.repository;

import au.edu.ardc.igsn.entity.Record;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface RecordRepository extends JpaRepository<Record, String> {

}
