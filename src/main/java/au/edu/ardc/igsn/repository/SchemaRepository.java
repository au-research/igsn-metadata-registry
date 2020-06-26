package au.edu.ardc.igsn.repository;

import au.edu.ardc.igsn.entity.Schema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchemaRepository extends PagingAndSortingRepository<Schema, String> {
}
