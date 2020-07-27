package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.TestHelper;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.model.DataCenter;
import au.edu.ardc.igsn.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ValidationService.class)
class ValidationServiceTest {

    @Autowired
    ValidationService service;

    @Test
    void validateRecordOwnership_validUserOwner_returnsTrue() {
        // given a record that is owned by an user
        User user = TestHelper.mockUser();
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        record.setOwnerID(user.getId());
        record.setOwnerType(Record.OwnerType.User);

        assertThat(service.validateRecordOwnership(record, user)).isTrue();
    }

    @Test
    void validateRecordOwnership_invalidUserOwner_returnsFalse() {
        // given a record that is owned by an user
        User user = TestHelper.mockUser();
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        record.setOwnerID(UUID.randomUUID());
        record.setOwnerType(Record.OwnerType.User);

        assertThat(service.validateRecordOwnership(record, user)).isFalse();
    }

    @Test
    void validateRecordOwnership_validDataCenterOwner_returnsTrue() {
        // given a record that is owned by an user
        DataCenter dataCenter = new DataCenter(UUID.randomUUID());
        User user = TestHelper.mockUser();
        user.setDataCenters(Arrays.asList(dataCenter));
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        record.setOwnerID(dataCenter.getId());
        record.setDataCenterID(dataCenter.getId());
        record.setOwnerType(Record.OwnerType.DataCenter);

        assertThat(service.validateRecordOwnership(record, user)).isTrue();
    }

    @Test
    void validateRecordOwnership_invalidDataCenterOwner_returnsTrue() {
        // given a record that is owned by an user
        DataCenter dataCenter = new DataCenter(UUID.randomUUID());
        User user = TestHelper.mockUser();
        user.setDataCenters(Arrays.asList(dataCenter));
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        record.setOwnerID(UUID.randomUUID());
        record.setDataCenterID(record.getOwnerID());
        record.setOwnerType(Record.OwnerType.DataCenter);

        assertThat(service.validateRecordOwnership(record, user)).isFalse();
    }

    @Test
    void validateAllocationScope_validAllocationScope_returnsTrue() {

    }
}