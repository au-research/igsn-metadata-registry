package au.edu.ardc.igsn;

import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.entity.Version;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Date;
import java.util.UUID;

public class TestHelper {

    public static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A helpful method to stub out a Record for testing
     *
     * @return a record with random things
     */
    public static Record mockRecord() {
        Record record = new Record();
        record = populateWithOwner(record, UUID.randomUUID());
        return record;
    }

    /**
     * Mock a record with a predefined UUID (not used for persisting)
     *
     * @param randomUUID some provided UUID
     * @return a record with random things
     */
    public static Record mockRecord(UUID randomUUID) {
        Record record = new Record(randomUUID);
        record = populateWithOwner(record, UUID.randomUUID());
        return record;
    }

    public static Record populateWithOwner(Record record, UUID creatorID) {
        record.setCreatorID(creatorID);
        record.setAllocationID(UUID.randomUUID());
        record.setDataCenterID(UUID.randomUUID());
        record.setOwnerID(creatorID);
        record.setOwnerType(Record.OwnerType.User);
        record.setModifiedAt(new Date());
        record.setCreatedAt(new Date());
        return record;
    }

    /**
     * Mock a version
     *
     * @return a Version with a mocked up record
     */
    public static Version mockVersion() {
        Record record = mockRecord();
        Version version = new Version(UUID.randomUUID());
        version.setCreatedAt(new Date());
        version.setStatus(Version.Status.CURRENT);
        version.setCreatorID(UUID.randomUUID());
        version.setRecord(record);
        version.setSchema("test-schema");
        return version;
    }

    /**
     * Mock a version
     *
     * @return a Version with a mocked up record
     */
    public static Version mockVersion(UUID id) {
        Record record = mockRecord(id);
        Version version = new Version(UUID.randomUUID());
        version.setCreatedAt(new Date());
        version.setStatus(Version.Status.CURRENT);
        version.setCreatorID(UUID.randomUUID());
        version.setRecord(record);
        version.setSchema("test-schema");
        return version;
    }

    /**
     * Mock a version given a record
     * @param record the record that the version will belong to
     * @return a Version
     */
    public static Version mockVersion(Record record) {
        Version version = new Version();
        version.setCreatedAt(new Date());
        version.setStatus(Version.Status.CURRENT);
        version.setRecord(record);
        version.setCreatorID(record.getCreatorID());
        return version;
    }


}
