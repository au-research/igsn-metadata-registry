package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.dto.IdentifierDTO;
import au.edu.ardc.igsn.dto.IdentifierMapper;
import au.edu.ardc.igsn.entity.Identifier;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.entity.Version;
import au.edu.ardc.igsn.exception.ForbiddenOperationException;
import au.edu.ardc.igsn.exception.RecordNotFoundException;
import au.edu.ardc.igsn.model.Allocation;
import au.edu.ardc.igsn.model.Scope;
import au.edu.ardc.igsn.model.User;
import au.edu.ardc.igsn.repository.IdentifierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdentifierService {

    @Autowired
    private IdentifierRepository repository;

    @Autowired
    private IdentifierMapper mapper;

    @Autowired
    private RecordService recordService;

    @Autowired
    private ValidationService validationService;

    /**
     * Find an identifier by id
     *
     * @param id the uuid of the Identifier
     * @return the identifier if it exists, null if not
     */
    public Identifier findById(String id) {
        Optional<Identifier> opt = repository.findById(UUID.fromString(id));

        return opt.orElse(null);
    }

    /**
     * Tell if an identifier exists by id
     *
     * @param id the uuid of the Identifier
     * @return if the uuid correlate to an existing version
     */
    public boolean exists(String id) {
        return repository.existsById(UUID.fromString(id));
    }

    /**
     * Retrieve all owned identifiers
     * Owned identifiers are the identifiers that which records the user have access to
     *
     * todo accept User UUID as a parameter
     * todo update findOwned at the repository level
     * @return a list of Identifiers that is owned by the user
     */
    public List<Identifier> findOwned() {
        return repository.findAll();
    }

    // create
    public Identifier create(Identifier newIdentifier) {
        return repository.save(newIdentifier);
    }

    public IdentifierDTO create(IdentifierDTO dto, User user) {
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

        return mapper.convertToDTO(identifier);
    }

    /**
     * Update a record
     *
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
     *
     * @param id the uuid of the Identifier
     */
    public void delete(String id) {
        repository.deleteById(id);
    }

}
