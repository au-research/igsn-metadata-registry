package au.edu.ardc.registry.oai.controller;

import au.edu.ardc.registry.common.config.ApplicationProperties;
import au.edu.ardc.registry.common.config.WebConfig;
import au.edu.ardc.registry.common.service.RecordService;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.service.VersionService;
import au.edu.ardc.registry.oai.model.IdentifyFragment;
import au.edu.ardc.registry.oai.response.OAIIdentifyResponse;
import au.edu.ardc.registry.oai.service.OAIPMHService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = OAIPMHController.class,
		excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = { WebConfig.class }))
@AutoConfigureMockMvc
class OAIPMHControllerTest {

	final String base_url = "/api/services/oai-pmh";

	@Autowired
	MockMvc mockMvc;

	@MockBean
	SchemaService schemaService;

	@MockBean
	RecordService recordService;

	@MockBean
	VersionService versionService;

	@MockBean
	ApplicationProperties applicationProperties;

	@MockBean
	OAIPMHService oaipmhService;

	@Test
	@DisplayName("Throws an exception and returns the error element with badverb code attribute")
	void test_handle_noVerbParam_throwsException() throws Exception {
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(base_url)
				.contentType(MediaType.APPLICATION_XML).accept(MediaType.APPLICATION_XML);

		mockMvc.perform(request).andDo(print()).andExpect(content().contentType(MediaType.APPLICATION_XML))
				.andExpect(xpath("/OAI-PMH/error[@code='badVerb']").exists()).andExpect(status().isOk());
	}

	@Test
	void handle_noVerb_throwsException() throws Exception {
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(base_url + "/?verb=nonsense")
				.contentType(MediaType.APPLICATION_XML).accept(MediaType.APPLICATION_XML);

		mockMvc.perform(request).andDo(print()).andExpect(content().contentType(MediaType.APPLICATION_XML))
				.andExpect(xpath("/OAI-PMH/error[@code='badVerb']").exists()).andExpect(status().isOk());
	}

}