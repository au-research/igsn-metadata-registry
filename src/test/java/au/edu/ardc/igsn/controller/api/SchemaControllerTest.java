package au.edu.ardc.igsn.controller.api;

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

    @Test
    public void it_should_get_all_supported_schemas() throws Exception {
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.get("/api/schemas/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                // .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").exists());
    }

    @Test
    public void it_should_get_a_single_schema() throws Exception {
        String schemaID = "csiro-igsn-v3";
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.get("/api/schemas/" + schemaID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                 .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("csiro-igsn-v3"));
    }

    @Test
    public void it_should_throw_notFound_if_there_is_no_such_schemaID() throws Exception {
        String schemaID = "non-exist";
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.get("/api/schemas/" + schemaID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isNotFound());
    }

}