package au.edu.ardc.registry.igsn.service;

import au.edu.ardc.registry.TestHelper;
import au.edu.ardc.registry.common.model.Allocation;
import au.edu.ardc.registry.common.model.Scope;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.igsn.config.IGSNApplicationConfig;
import au.edu.ardc.registry.igsn.model.IGSNAllocation;
import au.edu.ardc.registry.igsn.model.IGSNTask;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { IGSNService.class, SchemaService.class, IGSNApplicationConfig.class })
class IGSNServiceTest {

	@Autowired
	IGSNService igsnService;

	@MockBean
	IGSNRequestService igsnRequestService;

	@MockBean
	ImportService importService;

	@MockBean
	IGSNRegistrationService igsnRegistrationService;

	@BeforeEach
	void beforeEach() {
		igsnService.init();
	}

	@Test
	@DisplayName("Upon start up, the IGSNService should have the following queue setup")
	void init() {
		assertThat(igsnService.getSyncQueue()).isInstanceOf(BlockingQueue.class);
		assertThat(igsnService.getSyncQueue()).hasSize(0);
		assertThat(igsnService.getImportQueue()).isInstanceOf(Map.class);
	}

	@Test
	@DisplayName("Execute an Import Task will trigger importService.importRequest")
	void executeTask_ImportTask() throws IOException {

		// importService.importRequest returns a valid Identifier
		when(importService.importRequest(any(), any())).thenReturn(TestHelper.mockIdentifier());

		igsnService.executeTask(new IGSNTask(IGSNTask.TASK_IMPORT, "20.200.122/XX123435", UUID.randomUUID()));

		// importService.importRequest is called
		verify(importService, times(1)).importRequest(any(), any());
		verify(importService, times(0)).updateRequest(any(), any());
	}

	@Test
	@DisplayName("Execute an Import Task will trigger importService.importRequest")
	void executeTask_UpdateTask() throws IOException {

		// importService.importRequest returns a valid Identifier
		when(importService.updateRequest(any(), any())).thenReturn(TestHelper.mockIdentifier());

		igsnService.executeTask(new IGSNTask(IGSNTask.TASK_UPDATE, "20.200.122/XX123435", UUID.randomUUID()));

		// importService.importRequest is called
		verify(importService, times(1)).updateRequest(any(), any());
		verify(importService, times(0)).importRequest(any(), any());
	}

	@Test
	void hasIGSNTaskQueued_ImportQueue() {
		UUID allocationID = UUID.randomUUID();

		// given an empty queue
		assertThat(igsnService.hasIGSNTaskQueued(allocationID, IGSNTask.TASK_IMPORT, "identifierValue")).isFalse();

		// queue a task
		IGSNTask task = new IGSNTask(IGSNTask.TASK_IMPORT, new File("/tmp"), UUID.randomUUID());
		task.setIdentifierValue("identifierValue");
		igsnService.getImportQueueForAllocation(allocationID).add(task);

		// task of that type in queue is now available
		assertThat(igsnService.hasIGSNTaskQueued(allocationID, IGSNTask.TASK_IMPORT, "identifierValue")).isTrue();
	}

	@Test
	@DisplayName("Get IGSNAllocation gives null when user has the wrong allocation")
	void getIGSNAllocationForContent_mismatchUser_null() throws IOException {

		// given XML has 10273/XX0TUIAYLV
		String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");

		// given user that has allocation 20.500.11812/XXAA
		User user = TestHelper.mockUser();
		Allocation allocation = TestHelper.mockIGSNAllocation();
		allocation.setScopes(Arrays.asList(Scope.CREATE));
		user.setAllocations(Arrays.asList(allocation));

		assertThat(igsnService.getIGSNAllocationForContent(xml, user, Scope.CREATE)).isEqualTo(null);
	}

	@Test
	@DisplayName("Get IGSNAllocation returns proper IGSNAllocation when the prefix and namespace matches")
	void getIGSNAllocationForContent() throws IOException {

		// given XML has 10273/XX0TUIAYLV
		String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");

		// given user that has allocation 20.500.11812/XXAA
		User user = TestHelper.mockUser();
		Allocation allocation = TestHelper.mockIGSNAllocation();
		allocation.getAttributes().put("prefix", Collections.singletonList("10273"));
		allocation.getAttributes().put("namespace", Collections.singletonList("XX0TUIAYLV"));
		allocation.setAttributes(allocation.getAttributes());
		allocation.setScopes(Arrays.asList(Scope.CREATE, Scope.UPDATE));
		user.setAllocations(Collections.singletonList(allocation));

		assertThat(igsnService.getIGSNAllocationForContent(xml, user, Scope.CREATE)).isInstanceOf(IGSNAllocation.class);
		assertThat(igsnService.getIGSNAllocationForContent(xml, user, Scope.UPDATE)).isInstanceOf(IGSNAllocation.class);
	}

}