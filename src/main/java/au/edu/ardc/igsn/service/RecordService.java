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
     * @return A list of record by creator ID
     */
    public List<Record> findByCreatorID(String creatorID) {
        return repository.findByCreatedBy(creatorID);
    }

    // todo findOwned
    //  all record that has the creator_id the same as the current logged in user
    //  all record that has the allocation_id that the current logged in user has accesss to

    /**
     * Find a record by id
     *
     * @param id String representation of a uuid
     * @return the record if it exists, null if not
     */
    public Record findById(String id) {
        Optional<Record> opt = repository.findById(UUID.fromString(id).toString());

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
        return repository.existsById(id);
    }

    /**
     * Create a new record
     *
     * @param creatorID UUID of the user that created this record
     * @param allocationID UUID of the resource that allocates this record
     * @param ownerType the enumeration value of the OwnerType of this record
     * @return The record that was created
     */
    public Record create(String creatorID, String allocationID, Record.OwnerType ownerType) {

        Record record = new Record();
        record.setCreatedBy(creatorID);
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
    public Record update(Record record, String modifedBy) {

        record.setModifiedBy(modifedBy);
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
