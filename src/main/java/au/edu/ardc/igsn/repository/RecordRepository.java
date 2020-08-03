package au.edu.ardc.igsn.repository;

import au.edu.ardc.igsn.entity.Record;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface RecordRepository extends JpaRepository<Record, String> {

    List<Record> findByCreatorID(UUID creatorID);

    Optional<Record> findById(UUID id);

    boolean existsById(UUID id);

    @Query(value = "SELECT r FROM Record r WHERE r.creatorID = ?1 OR r.ownerID = ?1")
    List<Record> findOwned(UUID owner);

    @Query(value = "SELECT r FROM Record r WHERE r.creatorID = ?1 OR r.ownerID = ?1 OR r.allocationID IN ?2")
    List<Record> findOwned(UUID owner, List<UUID> allocationIDs, Pageable pageable);

    Page<Record> findAllByVisibleIsTrue(Pageable pageable);
}
