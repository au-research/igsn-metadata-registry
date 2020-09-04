package au.edu.ardc.registry.igsn.validator;

import au.edu.ardc.registry.TestHelper;
import au.edu.ardc.registry.common.dto.mapper.IdentifierMapper;
import au.edu.ardc.registry.common.dto.mapper.VersionMapper;
import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.common.repository.IdentifierRepository;
import au.edu.ardc.registry.common.repository.VersionRepository;
import au.edu.ardc.registry.common.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {VersionService.class, VersionContentValidator.class, ValidationService.class, IdentifierService.class})
class VersionContentValidatorTest {

    @Autowired
    VersionService vService;

    @Autowired
    VersionContentValidator vValidator;

    @MockBean
    IdentifierService iService;

    @MockBean
    VersionRepository repository;

    @MockBean
    IdentifierRepository iRepository;

    @MockBean
    IdentifierMapper iMapper;

    @MockBean
    VersionMapper mapper;

    @MockBean
    SchemaService sService;

    @MockBean
    RecordService rService;

    @MockBean
    ValidationService valService;

    @Test
    void isNewContent() {
        Version v = TestHelper.mockVersion();
        String oldContent = "fish";
        v.setContent(oldContent.getBytes());
        v.setHash(vService.getHash(v));
        boolean isNewContent = vValidator.isNewContent("fish", v);
        assertThat(isNewContent).isFalse();

    }

    @Test
    void isNewContent_DB()
    {
        String identifier = "20.500.11812/XXAB001QX";
        String schemaID = "ardc-igsn-desc-1.0";
        String newContent = "<resources></resources>";
        boolean isNewContent = vValidator.isNewContent(newContent, identifier, schemaID);
        assertThat(isNewContent).isTrue();

    }

}