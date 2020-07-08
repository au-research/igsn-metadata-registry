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
     * Create a new record
     *
     * @param creatorID UUID of the user that created this record
     * @param allocationID UUID of the resource that allocates this record
     * @param ownerType the enumeration value of the OwnerType of this record
     * @param datacenterID the UUID of the data center that the record could be owned by
     * @return The record that was created
     */
    public Record create(UUID creatorID, UUID allocationID, Record.OwnerType ownerType, UUID datacenterID) {

        Record record = new Record();
        record.setCreatorID(creatorID);
        record.setAllocationID(allocationID);

        record.setOwnerType(ownerType);
        if (ownerType.equals(Record.OwnerType.User)) {
            record.setOwnerID(creatorID);
        } else if(ownerType.equals(Record.OwnerType.DataCenter)) {
            record.setOwnerID(datacenterID);
        }

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
