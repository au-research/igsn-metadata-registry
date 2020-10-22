package au.edu.ardc.registry.common.service;

import au.edu.ardc.registry.common.dto.IdentifierDTO;
import au.edu.ardc.registry.common.dto.mapper.IdentifierMapper;
import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.model.Allocation;
import au.edu.ardc.registry.common.model.Scope;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.repository.IdentifierRepository;
import au.edu.ardc.registry.common.repository.specs.IdentifierSpecification;
import au.edu.ardc.registry.exception.ForbiddenOperationException;
import au.edu.ardc.registry.exception.RecordNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdentifierService {

	private final IdentifierRepository repository;

	private final IdentifierMapper mapper;

	private final RecordService recordService;

	private final ValidationService validationService;

	public IdentifierService(IdentifierRepository repository, IdentifierMapper mapper, RecordService recordService,
			ValidationService validationService) {
		this.repository = repository;
		this.mapper = mapper;
		this.recordService = recordService;
		this.validationService = validationService;
	}

	/**
	 * Search for {@link Identifier}
	 * @param specs the {@link IdentifierSpecification} for determining the filters
	 * @param pageable the {@link Pageable} for pagination and row limit
	 * @return a {@link Page} of {@link Identifier}
	 */
	public Page<Identifier> search(IdentifierSpecification specs, Pageable pageable) {
		return repository.findAll(specs, pageable);
	}

	/**
	 * Find an identifier by id
	 * @param id the uuid of the Identifier
	 * @return the identifier if it exists, null if not
	 */
	public Identifier findById(String id) {
		Optional<Identifier> opt = repository.findById(UUID.fromString(id));

		return opt.orElse(null);
	}

	public Identifier findByValueAndType(String value, Identifier.Type type) {
		return repository.findFirstByValueAndType(value, type);
	}

	/**
	 * Tell if an identifier exists by id
	 * @param id the uuid of the Identifier
	 * @return if the uuid correlate to an existing version
	 */
	public boolean exists(String id) {
		return repository.existsById(UUID.fromString(id));
	}

	public Identifier save(Identifier newIdentifier) {
		Identifier existingIdentifier = findByValueAndType(newIdentifier.getValue(), newIdentifier.getType());
		if (existingIdentifier != null) {
			throw new ForbiddenOperationException(String.format("Identifier {} with type {} already exists",
					newIdentifier.getValue(), newIdentifier.getType()));
		}
		return repository.saveAndFlush(newIdentifier);
	}

	public Identifier create(IdentifierDTO dto, User user) {
		Identifier identifier = mapper.convertToEntity(dto);

		// validate record existence
		if (!recordService.exists(dto.getRecord().toString())) {
			throw new RecordNotFoundException(dto.getRecord().toString());
		}

		// validate record ownership
		Record record = recordService.findById(dto.getRecord().toString());
		if (!validationService.validateRecordOwnership(record, user)) {
			throw new ForbiddenOperationException("User does not have access to create Identifier for this record");
		}

		// defaults
		identifier.setRecord(record);
		identifier.setCreatedAt(new Date());
		identifier.setUpdatedAt(new Date());

		// import scope overwrite
		Allocation allocation = new Allocation(record.getAllocationID());
		if (validationService.validateAllocationScope(allocation, user, Scope.IMPORT)) {
			identifier.setCreatedAt(dto.getCreatedAt() != null ? dto.getCreatedAt() : identifier.getCreatedAt());
			identifier.setUpdatedAt(dto.getUpdatedAt() != null ? dto.getUpdatedAt() : identifier.getUpdatedAt());
		}

		identifier = repository.save(identifier);

		return identifier;
	}

	/**
	 * Update a record
	 * @param identifier to be updated
	 * @return The identifier that has updated
	 */
	public Identifier update(Identifier identifier) {
		identifier.setUpdatedAt(new Date());
		repository.save(identifier);
		return identifier;
	}

	/**
	 * Permanently delete the identifier
	 * @param id the uuid of the Identifier
	 */
	public void delete(String id) {
		repository.deleteById(id);
	}

}
