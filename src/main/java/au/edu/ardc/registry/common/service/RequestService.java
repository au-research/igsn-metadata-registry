package au.edu.ardc.registry.common.service;

import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.repository.RequestRepository;
import au.edu.ardc.registry.common.repository.specs.RequestSpecification;
import au.edu.ardc.registry.exception.ForbiddenOperationException;
import au.edu.ardc.registry.exception.RequestNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class RequestService {

	final RequestRepository requestRepository;

	public RequestService(RequestRepository requestRepository) {
		this.requestRepository = requestRepository;
	}

	/**
	 * Find a request by id, created by a given user
	 * @param id uuid of the request
	 * @param user the creator {@link User}
	 * @return the {@link Request}
	 * @throws RequestNotFoundException when the request is not found
	 * @throws ForbiddenOperationException when the request is not owned by the user
	 */
	public Request findOwnedById(String id, User user) throws RequestNotFoundException, ForbiddenOperationException {
		Optional<Request> opt = requestRepository.findById(UUID.fromString(id));
		Request request = opt.orElseThrow(() -> new RequestNotFoundException(id));

		if (!request.getCreatedBy().equals(user.getId())) {
			throw new ForbiddenOperationException("User does not have access to this request");
		}

		return request;
	}

	/**
	 * Finds a {@link Request} by it's id
	 * @param id the UUID string
	 * @return the {@link Request}
	 */
	public Request findById(String id) {
		Optional<Request> opt = requestRepository.findById(UUID.fromString(id));
		return opt.orElse(null);
	}

	/**
	 * Performs a search based on the predefined search specification
	 * @param specs an instance of {@link RequestSpecification}
	 * @param pageable an instance of {@link Pageable}
	 * @return a {@link Page} of {@link Request}
	 */
	public Page<Request> search(Specification<Request> specs, Pageable pageable) {
		return requestRepository.findAll(specs, pageable);
	}

	// todo create
	// todo delete
	// todo update
}
