package au.edu.ardc.registry.common.repository;

import au.edu.ardc.registry.common.entity.Request;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RequestRepository extends JpaRepository<Request, String> {

	Optional<Request> findById(UUID id);

	boolean existsById(UUID id);

}
