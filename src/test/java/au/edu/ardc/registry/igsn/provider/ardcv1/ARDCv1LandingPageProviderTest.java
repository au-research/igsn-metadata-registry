package au.edu.ardc.registry.igsn.provider.ardcv1;

import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.provider.IdentifierProvider;
import au.edu.ardc.registry.common.provider.LandingPageProvider;
import au.edu.ardc.registry.common.provider.Metadata;
import au.edu.ardc.registry.common.provider.MetadataProviderFactory;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.util.Helpers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {SchemaService.class})
class ARDCv1LandingPageProviderTest {

    @Autowired
    SchemaService service;

    @Test
    @DisplayName("Get landing page From ARDCV1")
    public void getLandingPage() throws Exception
    {
        Schema schema = service.getSchemaByID(SchemaService.ARDCv1);
        String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
        LandingPageProvider provider = (LandingPageProvider) MetadataProviderFactory.create(schema, Metadata.LandingPage);
        String landingPageValue = provider.get(xml);
        assertEquals(landingPageValue, "https://demo.identifiers.ardc.edu.au/igsn/#/meta/XX0TUIAYLV");
    }

}