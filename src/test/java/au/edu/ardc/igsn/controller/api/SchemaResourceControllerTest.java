package au.edu.ardc.igsn.controller.api;

import au.edu.ardc.igsn.TestHelper;
import au.edu.ardc.igsn.entity.Schema;
import au.edu.ardc.igsn.service.SchemaService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class SchemaResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SchemaService service;

    @Test
    public void it_should_show_all_schemas() throws Exception {
        // given 2 schemas
        List<Schema> schemas = new ArrayList<>();
        schemas.add(new Schema("first", "First"));
        schemas.add(new Schema("second", "Second"));

        when(service.findAll()).thenReturn(schemas);

        mockMvc.perform(get("/api/resources/schemas/"))
                .andDo(print())
                .andExpect(jsonPath("$[*].id").isNotEmpty())
                .andExpect(status().isOk());
    }

    @Test
    public void it_should_find_one_schema() throws Exception {
        // given a schema
        when(service.findById("test")).thenReturn(new Schema("test", "Test"));

        // when showing that schema, the id and the name is available
        mockMvc.perform(get("/api/resources/schemas/test"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("test"))
                .andExpect(jsonPath("$.name").value("Test"))
                .andReturn();
    }

    @Test
    public void it_should_give_404_when_no_schema_is_found() throws Exception {
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.get("/api/resources/schemas/not-existed/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    //
    @Test
    public void it_should_add_schema_when_post() throws Exception {

        when(service.create(any(Schema.class))).thenReturn(new Schema("test", "Test"));

        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.post("/api/resources/schemas/")
                        .content(TestHelper.asJsonString(new Schema("test", "Test")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    public void it_should_update_the_schema_when_put() throws Exception {
        when(service.update(any(Schema.class))).thenReturn(new Schema("test", "Updated"));

        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.put("/api/resources/schemas/test")
                        .content(TestHelper.asJsonString(new Schema("test", "Updated")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andDo(print())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    public void it_should_delete_the_schema() throws Exception {
        when(service.delete("test")).thenReturn(true);

        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.delete("/api/resources/schemas/test")
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andDo(print())
                .andExpect(status().isAccepted());
    }

    // it_should_update_schema_when_put
    // it_should_delete_schema_when_delete
}

