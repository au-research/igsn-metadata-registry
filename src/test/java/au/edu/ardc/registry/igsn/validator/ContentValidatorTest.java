package au.edu.ardc.registry.igsn.validator;

import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.exception.ContentNotSupportedException;
import au.edu.ardc.registry.exception.VersionContentAlreadyExisted;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SchemaService.class)
class ContentValidatorTest {

	@Autowired
	SchemaService service;

	@Test
	public void failNoNamespace() throws IOException {
		ContentValidator cv = new ContentValidator(service);
		String validXML = Helpers.readFile("src/test/resources/xml/shiporder.xml");
		Assert.assertThrows(ContentNotSupportedException.class, () -> {
			boolean isValid = cv.validate(validXML);
		});
	}

	@Test
	public void failrifcsNameSpace() throws IOException {
		ContentValidator cv = new ContentValidator(service);
		String validXML = Helpers.readFile("src/test/resources/xml/rifcs_sample.xml");
		Assert.assertThrows(ContentNotSupportedException.class, () -> {
			boolean isValid = cv.validate(validXML);
		});

	}

}
