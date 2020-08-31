package au.edu.ardc.igsn.controller.api.services;

import au.edu.ardc.igsn.config.RequestLoggingFilter;
import au.edu.ardc.igsn.service.RecordService;
import au.edu.ardc.igsn.service.VersionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(
        controllers = OAIPMHService.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = RequestLoggingFilter.class
        )
)
@AutoConfigureMockMvc
class OAIPMHServiceTest {

    final String base_url = "/api/services/oai-pmh";

    @Autowired
    MockMvc mockMvc;

    @MockBean
    RecordService recordService;

    @MockBean
    VersionService versionService;

    @Test
    void handle_noVerbParam_throwsException() throws Exception {
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.get(base_url)
                        .contentType(MediaType.APPLICATION_XML)
                        .accept(MediaType.APPLICATION_XML);

        mockMvc.perform(request)
                .andDo(print())
                .andExpect(content().contentType(MediaType.APPLICATION_XML))
                .andExpect(status().isOk());
    }

    @Test
    void handle_noVerb_throwsException() throws Exception {
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.get(base_url + "/?verb=")
                        .contentType(MediaType.APPLICATION_XML)
                        .accept(MediaType.APPLICATION_XML);

        mockMvc.perform(request)
                .andExpect(content().contentType(MediaType.APPLICATION_XML))
                .andExpect(xpath("/OAI-PMH/error").string("Illegal OAI verb"))
                .andExpect(status().isOk());
    }

    @Test
    void handle_verb_Identify_returns() throws Exception {
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.get(base_url + "/?verb=Identify")
                        .contentType(MediaType.APPLICATION_XML)
                        .accept(MediaType.APPLICATION_XML);

        mockMvc.perform(request)
                .andExpect(content().contentType(MediaType.APPLICATION_XML))
                .andExpect(xpath("/OAI-PMH/Identify/repositoryName").string("ARDC IGSN Repository"))
                .andExpect(status().isOk());
    }

}