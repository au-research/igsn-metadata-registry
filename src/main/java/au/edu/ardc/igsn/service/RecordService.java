package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.model.Allocation;
import au.edu.ardc.igsn.model.Scope;
import au.edu.ardc.igsn.model.User;
import au.edu.ardc.igsn.dto.RecordDTO;
import au.edu.ardc.igsn.dto.RecordMapper;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.exception.ForbiddenOperationException;
import au.edu.ardc.igsn.exception.RecordNotFoundException;
import au.edu.ardc.igsn.repository.RecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
        if (!validateCreate(record, user)) {
            throw new ForbiddenOperationException("User does not have access to create record for this allocation");
        }

        // default record sets
        record.setCreatedAt(new Date());
        record.setCreatorID(user.getId());
        record.setOwnerType(Record.OwnerType.User);
        record.setOwnerID(user.getId());

        // allow igsn:import scope to overwrite default data
        if (validateImport(record, user)) {
            record.setCreatedAt(recordDTO.getCreatedAt() != null ? recordDTO.getCreatedAt() : record.getCreatedAt());
            record.setModifiedAt(recordDTO.getCreatedAt() != null ? recordDTO.getModifiedAt() : record.getModifiedAt());
            record.setCreatorID(recordDTO.getCreatorID() != null ? recordDTO.getCreatorID() : record.getCreatorID());
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
        validateUpdate(record, user);
        repository.delete(record);
        return true;
    }

    /**
     * Validates if the user owns the record
     * The user owned the record if they have access to the allocation
     *
     * @param record Record
     * @param user User model
     * @return true if user has access
     */
    public boolean validate(Record record, User user) {
        String allocationID = record.getAllocationID().toString();
        if (!user.hasPermission(allocationID)) {
            throw new ForbiddenOperationException(String.format("Insufficient permission, required access to resource %s", allocationID));
        }
        return true;
    }

    public boolean validateUpdate(Record record, User user) {
        if (record.getOwnerType().equals(Record.OwnerType.User) && record.getOwnerID().equals(user.getId())) {
            return true;
        }

        if (record.getOwnerType().equals(Record.OwnerType.DataCenter) && user.belongsToDataCenter(record.getOwnerID())) {
            return true;
        }

        throw new ForbiddenOperationException("You don't have permission to update this record");
    }

    public boolean validateCreate(Record record, User user) {
        // todo simplify
        // if the user has igsn:create scope according to the claimed allocation
        UUID allocationID = record.getAllocationID();
        if (!user.hasAllocation(allocationID)) {
            return false;
        }
        Allocation userAllocation = user.getAllocationById(allocationID);

        if (userAllocation.getScopes().contains(Scope.CREATE)) {
            return true;
        }
        return false;
    }

    public boolean validateImport(Record record, User user) {
        UUID allocationID = record.getAllocationID();
        if (!user.hasAllocation(allocationID)) {
            return false;
        }
        Allocation userAllocation = user.getAllocationById(allocationID);

        if (userAllocation.getScopes().contains(Scope.IMPORT)) {
            return true;
        }
        return false;
    }

    /**
     * Validates if the User has access to the allocation and the required scope
     *
     * @param record Record
     * @param user User
     * @param scope Scope
     * @return true if user has access
     */
    public boolean validate(Record record, User user, Scope scope) {
        String allocationID = record.getAllocationID().toString();
        if (!user.hasPermission(allocationID, scope)) {
            throw new ForbiddenOperationException(String.format("Insufficient permission, required %s on resource %s", scope.getValue(), allocationID));
        }
        return true;
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
        validateUpdate(record, user);

        // can update Status, AllocationID, OwnerID and OwnerType
        record.setStatus(recordDTO.getStatus() != null ? recordDTO.getStatus() : record.getStatus());
        record.setAllocationID(recordDTO.getAllocationID() != null ? recordDTO.getAllocationID() : record.getAllocationID());
        record.setOwnerID(recordDTO.getOwnerID() != null ? recordDTO.getOwnerID() : record.getOwnerID());
        record.setOwnerType(recordDTO.getOwnerType() != null ? recordDTO.getOwnerType() : record.getOwnerType());

        // modification timestamp
        record.setModifierID(user.getId());
        record.setModifiedAt(new Date());

        // allow igsn:import scope to overwrite certain fields
        if (validateImport(record, user)) {
            record.setCreatedAt(recordDTO.getCreatedAt() != null ? recordDTO.getCreatedAt() : record.getCreatedAt());
            record.setModifiedAt(recordDTO.getModifiedAt() != null ? recordDTO.getModifiedAt() : record.getCreatedAt());
            record.setCreatorID(recordDTO.getCreatorID() != null ? recordDTO.getCreatorID() : record.getCreatorID());
        }

        record = repository.save(record);

        return mapper.convertToDTO(record);
    }

}
