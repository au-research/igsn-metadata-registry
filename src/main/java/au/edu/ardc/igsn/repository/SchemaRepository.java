package au.edu.ardc.igsn.repository;

import au.edu.ardc.igsn.entity.SchemaEntity;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchemaRepository extends PagingAndSortingRepository<SchemaEntity, String> {
}
