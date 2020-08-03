package au.edu.ardc.igsn.controller.api;

import au.edu.ardc.igsn.util.Helpers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class SchemaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final String baseUrl = "/api/resources/schemas/";

    @Test
    public void it_should_get_all_supported_schemas() throws Exception {
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.get(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                // .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").exists());
    }

    @Test
    public void it_should_get_a_single_schema() throws Exception {
        String schemaID = "igsn-csiro-v3-descriptive";
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.get(baseUrl + schemaID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("igsn-csiro-v3-descriptive"));
    }

    @Test
    public void it_should_throw_notFound_if_there_is_no_such_schemaID() throws Exception {
        String schemaID = "non-exist";
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.get(baseUrl + schemaID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isNotFound());
    }

    @Test
    public void it_should_validate_schema() throws Exception {
        String validXML = Helpers.readFile("src/test/resources/xml/sample_igsn_csiro_v3.xml");
        String invalidXML = Helpers.readFile("src/test/resources/xml/invalid_sample_igsn_csiro_v3.xml");

        String schemaID = "igsn-csiro-v3-descriptive";
        MockHttpServletRequestBuilder validRequest =
                MockMvcRequestBuilders.post(baseUrl + schemaID + "/validate")
                        .content(validXML)
                        .contentType(MediaType.APPLICATION_XML)
                        .accept(MediaType.APPLICATION_JSON);

        MockHttpServletRequestBuilder invalidRequest =
                MockMvcRequestBuilders.post(baseUrl + schemaID + "/validate")
                        .content(invalidXML)
                        .contentType(MediaType.APPLICATION_XML)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(validRequest).andExpect(status().isOk());
        mockMvc.perform(invalidRequest).andExpect(status().isBadRequest());
    }

    @Test
    public void it_should_throw_404_if_attempt_to_validate_nonexistent_schema() throws Exception {
        String validXML = Helpers.readFile("src/test/resources/xml/sample_igsn_csiro_v3.xml");

        String schemaID = "non-existence";
        MockHttpServletRequestBuilder validRequest =
                MockMvcRequestBuilders.post(baseUrl + schemaID + "/validate")
                        .content(validXML)
                        .contentType(MediaType.APPLICATION_XML)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(validRequest).andExpect(status().isNotFound());
    }


}