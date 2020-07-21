package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.entity.Record;
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

    /**
     * Returns all Records
     *
     * @param creatorID the String uuid of the creator
     * @return A list of record by creator ID
     */
    public List<Record> findByCreatorID(String creatorID) {
        return repository.findByCreatorID(UUID.fromString(creatorID));
    }

    /**
     * Returns all record that the user created
     * Returns all record that the user owned
     * Returns all record that the user has access to via allocation
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
     * Persist a newRecord
     *
     * @param newRecord a Valid Record
     * @return the newly persisted record with updated uuid
     */
    public Record create(Record newRecord) {
        return repository.save(newRecord);
    }

    /**
     * Create a record with the owner
     * defaults to owned by the user
     *
     * @param ownerID UUID of the owner
     * @param allocationID UUID of the allocation
     * @return newly persisted record
     */
    public Record create(UUID ownerID, UUID allocationID) {
        Record record = new Record();
        record.setCreatorID(ownerID);
        record.setOwnerType(Record.OwnerType.User);
        record.setOwnerID(ownerID);
        record.setAllocationID(allocationID);
        record.setCreatedAt(new Date());
        record.setModifiedAt(new Date());
        return repository.save(record);
    }

    /**
     * Update a record
     *
     * @param record full record to be updated, including all NotNull fields
     * @param modifierID the UUID of the user who modifies this record
     * @return The record that has updated
     */
    public Record update(Record record, UUID modifierID) {

        record.setModifierID(modifierID);
        record.setModifiedAt(new Date());
        repository.save(record);

        return record;
    }

    /**
     * Delete a record
     *
     * @param recordTobeDeleted the Record to be deleted
     * @return true if the delete is successful
     */
    public boolean delete(Record recordTobeDeleted) {
        // todo soft delete
        repository.delete(recordTobeDeleted);

        // todo handle cascaded entities
        return true;
    }
}
