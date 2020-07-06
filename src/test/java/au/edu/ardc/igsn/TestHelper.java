package au.edu.ardc.igsn;

import au.edu.ardc.igsn.entity.Record;
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
        Record record = new Record(UUID.randomUUID());
        record.setCreatorID(UUID.randomUUID());
        record.setAllocationID(UUID.randomUUID());
        record.setDataCenterID(UUID.randomUUID());
        record.setOwnerID(UUID.randomUUID());
        record.setOwnerType(Record.OwnerType.User);
        record.setUpdatedAt(new Date());
        record.setCreatedAt(new Date());
        return record;
    }
}
