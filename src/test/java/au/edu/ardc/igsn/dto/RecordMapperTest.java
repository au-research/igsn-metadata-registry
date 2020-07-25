package au.edu.ardc.igsn.dto;

import au.edu.ardc.igsn.TestHelper;
import au.edu.ardc.igsn.entity.Record;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@BootstrapWith(SpringBootTestContextBootstrapper.class)
public class RecordMapperTest {

    @Autowired
    RecordMapper mapper;

    @Test
    void convertToEntity() {
        RecordDTO dto = new RecordDTO();
        dto.setAllocationID(UUID.randomUUID());
        dto.setOwnerType(Record.OwnerType.User);
        dto.setOwnerID(UUID.randomUUID());
        dto.setCreatedAt(new Date());
        dto.setModifiedAt(new Date());
        dto.setModifierID(UUID.randomUUID());
        dto.setCreatedAt(new Date());

        Record actual = mapper.convertToEntity(dto);
        assertThat(dto.getAllocationID()).isEqualTo(actual.getAllocationID());
        assertThat(dto.getOwnerType()).isEqualTo(actual.getOwnerType());
        assertThat(dto.getOwnerID()).isEqualTo(actual.getOwnerID());
        assertThat(dto.getCreatorID()).isEqualTo(actual.getCreatorID());
        assertThat(dto.getModifiedAt()).isEqualTo(actual.getModifiedAt());
        assertThat(dto.getModifierID()).isEqualTo(actual.getModifierID());
        assertThat(dto.getCreatedAt()).isEqualTo(actual.getCreatedAt());
    }

    @Test
    void convertToDTO() {
        Record record = TestHelper.mockRecord();

        RecordDTO dto = mapper.convertToDTO(record);
        assertThat(dto.getId()).isEqualTo(record.getId());
        assertThat(dto.getAllocationID()).isEqualTo(record.getAllocationID());
        assertThat(dto.getOwnerType()).isEqualTo(record.getOwnerType());
        assertThat(dto.getOwnerID()).isEqualTo(record.getOwnerID());
        assertThat(dto.getCreatorID()).isEqualTo(record.getCreatorID());
        assertThat(dto.getModifiedAt()).isEqualTo(record.getModifiedAt());
        assertThat(dto.getModifierID()).isEqualTo(record.getModifierID());
        assertThat(dto.getCreatedAt()).isEqualTo(record.getCreatedAt());
        assertThat(dto.getStatus()).isEqualTo(record.getStatus());
    }
}