package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.repository.RecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RecordService {

    @Autowired
    private RecordRepository repository;

    @Autowired
    private KeycloakService kcService;

    /**
     * Returns all Records
     *
     * @return A list of record by creator ID
     */
    public List<Record> findByCreatorID(String creatorID) {
        return repository.findByCreatorID(UUID.fromString(creatorID));
    }

    /**
     * Returns all records
     *
     * @return a list of all records available in the registry
     */
    public List<Record> findAll() {
        return repository.findAll();
    }

    /**
     * Returns all record that the user created
     * Returns all record that the user owned
     * Returns all record that the user has access to via allocation
     *
     * @param request The current HttpServletRequest
     * @return a list of records that the currently logged in user owned
     */
    public List<Record> findOwned(HttpServletRequest request) {
         UUID ownerID = kcService.getUserUUID(request);

        return repository.findOwned(ownerID);
    }


    /**
     * Find a record by id
     *
     * @param id String representation of a uuid
     * @return the record if it exists, null if not
     */
    public Record findById(String id) {
        Optional<Record> opt = repository.findById(UUID.fromString(id));

        return opt.orElse(null);
    }

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
     * Create a new record
     *
     * @param creatorID UUID of the user that created this record
     * @param allocationID UUID of the resource that allocates this record
     * @param ownerType the enumeration value of the OwnerType of this record
     * @return The record that was created
     */
    public Record create(UUID creatorID, UUID allocationID, Record.OwnerType ownerType) {

        Record record = new Record();
        record.setCreatorID(creatorID);
        record.setAllocationID(allocationID);
        record.setOwnerType(ownerType);

        record.setCreatedAt(new Date());
        record.setUpdatedAt(new Date());

        return repository.save(record);
    }

    /**
     * Update a record
     *
     * @param record full record to be updated, including all NotNull fields
     * @return The record that has updated
     */
    public Record update(Record record, UUID modifierID) {

        record.setModifierID(modifierID);
        record.setUpdatedAt(new Date());
        repository.save(record);

        return record;
    }

    /**
     * Delete a record
     *
     * @param recordTobeDeleted the Record to be deleted
     * @return
     */
    public boolean delete(Record recordTobeDeleted) {
        // todo soft delete
        repository.delete(recordTobeDeleted);

        // todo handle cascaded entities
        return true;
    }
}
