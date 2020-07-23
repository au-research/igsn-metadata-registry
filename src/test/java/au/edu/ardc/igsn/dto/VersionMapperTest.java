package au.edu.ardc.igsn.dto;

import au.edu.ardc.igsn.TestHelper;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.entity.Version;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@RunWith(SpringRunner.class)
@BootstrapWith(SpringBootTestContextBootstrapper.class)
public class VersionMapperTest {

    @Autowired
    VersionMapper mapper;

    @Test
    public void it_should_convert_dto_to_entity() {
        VersionDTO dto = new VersionDTO();
        dto.setSchema("test-schema");
        dto.setRecord(UUID.randomUUID().toString());
        dto.setContent(Base64.getEncoder().encodeToString("stuff".getBytes()));

        Version actual = mapper.convertToEntity(dto);
        assertThat(actual.getSchema()).isEqualTo(dto.getSchema());
        assertThat(actual.getRecord()).isNull();
        assertThat(actual.getContent()).isNotEmpty();
    }

    @Test
    public void it_should_convert_entity_to_dto() {
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        Version version = TestHelper.mockVersion(UUID.randomUUID());
        version.setRecord(record);

        VersionDTO dto = mapper.convertToDTO(version);
        assertThat(dto.getId()).isEqualTo(version.getId().toString());
        assertThat(dto.getSchema()).isEqualTo(version.getSchema());
        assertThat(dto.getRecord()).isEqualTo(record.getId().toString());
    }

}