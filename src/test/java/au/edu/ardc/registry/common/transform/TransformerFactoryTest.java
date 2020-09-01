package au.edu.ardc.registry.common.transform;

import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.igsn.transform.ardcv1.ARDCv1ToJSONLDTransformer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {SchemaService.class})
class TransformerFactoryTest {
    @Autowired
    SchemaService schemaService;

    @Test
    @DisplayName("Returns the right class for converting between ardcv1 and ardcv1jsonld")
    void create_SchemaARDCv1ToJSONLD_returnsRightClass() {
        Schema fromSchema = schemaService.getSchemaByID(SchemaService.ARDCv1);
        Schema toSchema = schemaService.getSchemaByID(SchemaService.ARDCv1JSONLD);
        Transformer transformer = (Transformer) TransformerFactory.create(fromSchema, toSchema);

        assertThat(transformer).isInstanceOf(Transformer.class);
        assertThat(transformer).isInstanceOf(ARDCv1ToJSONLDTransformer.class);
    }
}