package au.edu.ardc.igsn.repository;

import au.edu.ardc.igsn.entity.Record;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface RecordRepository extends JpaRepository<Record, String> {

    List<Record> findByCreatedBy(String jackUUID);
}
