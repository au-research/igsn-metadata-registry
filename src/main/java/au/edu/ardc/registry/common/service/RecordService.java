package au.edu.ardc.registry.common.service;

import au.edu.ardc.registry.common.model.Allocation;
import au.edu.ardc.registry.common.model.Scope;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.dto.RecordDTO;
import au.edu.ardc.registry.common.dto.mapper.RecordMapper;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.exception.ForbiddenOperationException;
import au.edu.ardc.registry.exception.RecordNotFoundException;
import au.edu.ardc.registry.common.repository.RecordRepository;
import au.edu.ardc.registry.common.repository.specs.RecordSpecification;
import au.edu.ardc.registry.common.repository.specs.SearchCriteria;
import au.edu.ardc.registry.common.repository.specs.SearchOperation;
import com.google.common.base.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RecordService {

    @Autowired
    private RecordRepository repository;

    @Autowired
    private RecordMapper mapper;

    @Autowired
    private ValidationService validationService;

    /**
     * Returns all record that the user created
     * Returns all record that the user owned
     * Returns all record that the user has access to via allocation
     * todo refactor to return a page of RecordDTO
     * todo refactor to use Pageable
     *
     * @param ownerID The current loggedIn user UUID
     * @return a list of records that the currently logged in user owned
     */
    public List<Record> findOwned(UUID ownerID) {
        // todo findOwned by user ID as well as allocation IDs

        return repository.findOwned(ownerID);
    }

    public Page<RecordDTO> search(Specification<Record> specs, Pageable pageable) {
        Page<Record> result = repository.findAll(specs, pageable);
        return result.map(getDTOConverter());
    }

    /**
     * Return all publicly available records
     *
     * @param pageable Pageable
     * @return a list of RecordDTO that fits the criteria
     */
    public Page<RecordDTO> findAllPublic(Pageable pageable) {
        RecordSpecification visibleSpec = new RecordSpecification();
        visibleSpec.add(new SearchCriteria("visible", true, SearchOperation.EQUAL));
        return search(visibleSpec, pageable);
    }

    /**
     * Reusable DTO converter for use within the class
     *
     * @return Converter between Record and RecordDTO
     */
    private Converter<Record, RecordDTO> getDTOConverter() {
        return new Converter<Record, RecordDTO>() {
            @Override
            protected RecordDTO doForward(Record record) {
                return mapper.convertToDTO(record);
            }

            @Override
            protected Record doBackward(RecordDTO recordDTO) {
                return mapper.convertToEntity(recordDTO);
            }
        };
    }

    /**
     * Find and return a publicly available record
     *
     * @param id uuid of the record
     * @return RecordDTO
     */
    public RecordDTO findPublicById(String id) {
        Record record = findById(id);
        if (record == null || !record.isVisible()) {
            throw new RecordNotFoundException(id);
        }
        return mapper.convertToDTO(record);
    }

    /**
     * Find a record by id
     * todo unit test
     * @param id String representation of a uuid
     * @return the record if it exists, null if not
     */
    public Record findById(String id) {
        Optional<Record> opt = repository.findById(UUID.fromString(id));

        return opt.orElse(null);
    }

    /**
     * todo unit test
     *
     * @param id uuid of the record
     * @param user the current logged in user
     * @return RecordDTO
     */
    public RecordDTO findById(String id, User user) {
        Optional<Record> opt = repository.findById(UUID.fromString(id));
        Record record = opt.orElseThrow(() -> new RecordNotFoundException(id));
        return mapper.convertToDTO(record);
    }

    // todo List<RecordDTO> findOwnedBy(User)
    // todo List<RecordDTO> findCreatedBy(User)

    /**
     * Tell if a record exists by id
     * todo handle soft delete
     *
     * @param id String uuid
     * @return if the uuid correlate to an existing record
     */
    public boolean exists(String id) {
        return repository.existsById(UUID.fromString(id));
    }

    /**
     * Creates the Record
     *
     * @param recordDTO Validated RecordDTO
     * @param user User Model
     * @return RecordDTO if the creation is successful
     */
    public RecordDTO create(RecordDTO recordDTO, User user) {
        // recordDTO should already be @Valid
        Record record = mapper.convertToEntity(recordDTO);

        // validate user access
        Allocation allocation = new Allocation(record.getAllocationID());
        if (!validationService.validateAllocationScope(allocation, user, Scope.CREATE)) {
            throw new ForbiddenOperationException("User does not have access to create record for this allocation");
        }

        // default record sets
        record.setCreatedAt(new Date());
        record.setCreatorID(user.getId());
        record.setOwnerType(Record.OwnerType.User);
        record.setOwnerID(user.getId());
        record.setVisible(true);

        // allow igsn:import scope to overwrite default data
        if (validationService.validateAllocationScope(allocation, user, Scope.IMPORT)) {
            record.setVisible(recordDTO.isVisible());
            record.setCreatedAt(recordDTO.getCreatedAt() != null ? recordDTO.getCreatedAt() : record.getCreatedAt());
            record.setModifiedAt(recordDTO.getCreatedAt() != null ? recordDTO.getModifiedAt() : record.getModifiedAt());
            record.setCreatorID(recordDTO.getCreatorID() != null ? recordDTO.getCreatorID() : record.getCreatorID());
            record.setOwnerID(recordDTO.getOwnerID() != null ? recordDTO.getOwnerID() : record.getOwnerID());
            record.setOwnerType(recordDTO.getOwnerType() != null ? recordDTO.getOwnerType() : record.getOwnerType());
        }

        record = repository.save(record);

        return mapper.convertToDTO(record);
    }

    /**
     * Deletes a record completely from the repository
     *
     * @param id the id of the record to be deleted
     * @param user The User Model
     * @return true if the record is deleted
     */
    public boolean delete(String id, User user) {
        if (!exists(id)) {
            throw new RecordNotFoundException(id);
        }
        Record record = findById(id);
        if (!validationService.validateRecordOwnership(record, user)) {
            throw new ForbiddenOperationException("You don't have permission to update this record");
        }
        repository.delete(record);
        return true;
    }

    public RecordMapper getMapper() {
        return mapper;
    }

    /**
     * Persist a newRecord
     * todo refactor remove
     * @param newRecord a Valid Record
     * @return the newly persisted record with updated uuid
     */
    public Record create(Record newRecord) {
        return repository.save(newRecord);
    }

    /**
     * Updates a record
     * Validates record existence
     * Validates User ownership
     *
     * @param recordDTO the dto of the record
     * @param user User model
     * @return RecordDTO
     */
    public RecordDTO update(RecordDTO recordDTO, User user) {
        if (!exists(recordDTO.getId().toString())) {
            throw new RecordNotFoundException(recordDTO.getId().toString());
        }
        Record record = findById(recordDTO.getId().toString());

        if (!validationService.validateRecordOwnership(record, user)) {
            throw new ForbiddenOperationException("You don't have permission to update this record");
        }

        // can update
        record.setVisible(recordDTO.isVisible());
        record.setAllocationID(recordDTO.getAllocationID() != null ? recordDTO.getAllocationID() : record.getAllocationID());
        record.setOwnerID(recordDTO.getOwnerID() != null ? recordDTO.getOwnerID() : record.getOwnerID());
        record.setOwnerType(recordDTO.getOwnerType() != null ? recordDTO.getOwnerType() : record.getOwnerType());

        // modification timestamp
        record.setModifierID(user.getId());
        record.setModifiedAt(new Date());

        // allow igsn:import scope to overwrite certain fields
        if (validationService.validateAllocationScope(new Allocation(record.getAllocationID()), user, Scope.IMPORT)) {
            record.setCreatedAt(recordDTO.getCreatedAt() != null ? recordDTO.getCreatedAt() : record.getCreatedAt());
            record.setModifiedAt(recordDTO.getModifiedAt() != null ? recordDTO.getModifiedAt() : record.getCreatedAt());
            record.setCreatorID(recordDTO.getCreatorID() != null ? recordDTO.getCreatorID() : record.getCreatorID());
            record.setModifierID(recordDTO.getModifierID() != null ? recordDTO.getModifierID() : record.getModifierID());
        }

        record = repository.save(record);

        return mapper.convertToDTO(record);
    }

    public Record save(Record record) {
        return repository.saveAndFlush(record);
    }
}
