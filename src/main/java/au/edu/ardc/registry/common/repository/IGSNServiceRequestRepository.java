package au.edu.ardc.registry.common.repository;

import au.edu.ardc.registry.igsn.entity.IGSNServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IGSNServiceRequestRepository extends JpaRepository<IGSNServiceRequest, String> {

	Optional<IGSNServiceRequest> findById(UUID id);

	boolean existsById(UUID id);

}
