package au.edu.ardc.registry.igsn.service;

import au.edu.ardc.registry.TestHelper;
import au.edu.ardc.registry.common.entity.*;
import au.edu.ardc.registry.common.model.Attribute;
import au.edu.ardc.registry.common.service.*;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.exception.ForbiddenOperationException;
import au.edu.ardc.registry.exception.VersionContentAlreadyExistsException;
import org.apache.logging.log4j.Level;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ImportService.class, SchemaService.class })
class ImportServiceTest {

	@Autowired
	ImportService importService;

	@MockBean
	IGSNRequestService igsnRequestService;

	@MockBean
	IdentifierService identifierService;

	@MockBean
	RecordService recordService;

	@MockBean
	IGSNVersionService igsnVersionService;

	@MockBean
	URLService urlService;

	@Test
	@DisplayName("Import valid payload and valid request")
	void importRequest_validCreation() throws IOException {
		when(igsnRequestService.getLoggerFor(any(Request.class)))
				.thenReturn(TestHelper.getConsoleLogger(ImportServiceTest.class.getName(), Level.DEBUG));

		Request request = TestHelper.mockRequest();
		request.setAttribute(Attribute.CREATOR_ID, UUID.randomUUID().toString());
		request.setAttribute(Attribute.ALLOCATION_ID, UUID.randomUUID().toString());

		// when importRequest a valid payload and a valid request
		Identifier result = importService.importRequest(new File("src/test/resources/xml/sample_ardcv1.xml"), request);

		assertThat(result).isInstanceOf(Identifier.class);

		// 1 of each are created
		verify(recordService, times(1)).save(any(Record.class));
		verify(identifierService, times(1)).save(any(Identifier.class));
		verify(igsnVersionService, times(1)).save(any(Version.class));
	}

	@Test
	@DisplayName("Import valid payload but error creating Identifier should returns null and delete Record")
	void importRequest_errorCreatingIdentifier_returnsNull() throws IOException {
		when(igsnRequestService.getLoggerFor(any(Request.class)))
				.thenReturn(TestHelper.getConsoleLogger(ImportServiceTest.class.getName(), Level.DEBUG));
		when(identifierService.save(any(Identifier.class))).thenThrow(new ForbiddenOperationException("bad"));

		Request request = TestHelper.mockRequest();
		request.setAttribute(Attribute.OWNER_TYPE, "User");
		request.setAttribute(Attribute.CREATOR_ID, UUID.randomUUID().toString());
		request.setAttribute(Attribute.ALLOCATION_ID, UUID.randomUUID().toString());

		// when importRequest a valid payload and a valid request
		Identifier result = importService.importRequest(new File("src/test/resources/xml/sample_ardcv1.xml"), request);

		assertThat(result).isNull();

		// record is saved first, then deleted
		verify(recordService, times(1)).save(any(Record.class));
		verify(recordService, times(1)).delete(any(Record.class));
	}

	@Test
	@DisplayName("Update valid payload calls the right save method")
	void updateRequest() throws IOException {
		when(igsnRequestService.getLoggerFor(any(Request.class)))
				.thenReturn(TestHelper.getConsoleLogger(ImportServiceTest.class.getName(), Level.DEBUG));

		// given an existing Identifier and an existing Record
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		Identifier identifier = TestHelper.mockIdentifier(record);
		identifier.setValue("10273/XX0TUIAYLV");
		Version version = TestHelper.mockVersion(record);
		version.setSchema(SchemaService.ARDCv1);
		record.setCurrentVersions(Collections.singletonList(version));

		when(identifierService.findByValueAndType(identifier.getValue(), Identifier.Type.IGSN)).thenReturn(identifier);

		Request request = TestHelper.mockRequest();
		request.setAttribute(Attribute.OWNER_TYPE, "User");
		request.setAttribute(Attribute.CREATOR_ID, UUID.randomUUID().toString());
		request.setAttribute(Attribute.ALLOCATION_ID, UUID.randomUUID().toString());

		// when importRequest a valid payload and a valid request
		Identifier result = importService.updateRequest(new File("src/test/resources/xml/sample_ardcv1.xml"), request);

		assertThat(result).isInstanceOf(Identifier.class);

		verify(recordService, times(1)).save(any(Record.class));
		verify(igsnVersionService, times(1)).save(any(Version.class));
	}

	@Test
	@DisplayName("Update similar content already exists throws VersionContentAlreadyExistsException")
	void updateRequest_previousVersionSameHash_throwsException() throws IOException {
		when(igsnRequestService.getLoggerFor(any(Request.class)))
				.thenReturn(TestHelper.getConsoleLogger(ImportServiceTest.class.getName(), Level.DEBUG));

		// given an existing Identifier and an existing Record
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		Identifier identifier = TestHelper.mockIdentifier(record);
		identifier.setValue("10273/XX0TUIAYLV");
		Version oldVersion = TestHelper.mockVersion(record);
		oldVersion.setSchema(SchemaService.ARDCv1);
		oldVersion.setHash(VersionService.getHash(Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml")));
		record.setCurrentVersions(Collections.singletonList(oldVersion));

		when(identifierService.findByValueAndType(identifier.getValue(), Identifier.Type.IGSN)).thenReturn(identifier);

		Request request = TestHelper.mockRequest();
		request.setAttribute(Attribute.CREATOR_ID, UUID.randomUUID().toString());

		Assert.assertThrows(VersionContentAlreadyExistsException.class, () -> {
			importService.updateRequest(new File("src/test/resources/xml/sample_ardcv1.xml"), request);
		});

	}

}