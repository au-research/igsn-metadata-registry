package au.edu.ardc.igsn.controller.api;

import au.edu.ardc.igsn.TestHelper;
import au.edu.ardc.igsn.entity.Schema;
import au.edu.ardc.igsn.service.SchemaService;
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
import org.springframework.transaction.annotation.Transactional;

import static au.edu.ardc.igsn.TestHelper.asJsonString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class SchemaResourceControllerIT {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SchemaService service;

    @Test
    @Transactional
    public void it_should_show_all_schemas() throws Exception {
        // given 2 schemas
        service.create(new Schema("first", "First"));
        service.create(new Schema("second", "Second"));

        // when GET /api/resources/schemas/, see that they're there
        mockMvc.perform(get("/api/resources/schemas/"))
//                .andDo(print())
                .andExpect(jsonPath("$[*].id").isNotEmpty())
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    public void it_should_show_one_schema() throws Exception {
        // given a schema
        service.create(new Schema("test", "Show"));

        // when GET /{id}
        mockMvc.perform(get("/api/resources/schemas/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("test"))
                .andExpect(jsonPath("$.name").value("Show"));
    }

    @Test
    @Transactional
    public void it_should_add_schema_when_post() throws Exception {
        // given that schema `test` doesn't exist
        assertThat(service.findById("test")).isNull();

        // when POST to /
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.post("/api/resources/schemas/")
                        .content(asJsonString(new Schema("test", "Test")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());

        // schema `test` now exist
        assertThat(service.findById("test")).isNotNull();
        assertThat(service.findById("test")).isInstanceOf(Schema.class);
    }

    @Test
    @Transactional
    public void it_should_show_validation_error() throws Exception {
        assertThat(service.findById("test")).isNull();

        Schema invalidSchema = new Schema();
        invalidSchema.setId("test");

        // when POST to /
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.post("/api/resources/schemas/")
                        .content(asJsonString(invalidSchema))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.name").value("Name is mandatory"));

        // schema is not created
        assertThat(service.findById("test")).isNull();
    }

    @Test
    @Transactional
    public void it_should_update_schema_when_put() throws Exception {
        // given a schema
        service.create(new Schema("test", "Original"));

        // when PUT to /{id}
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

        // schema now has updated name
        Schema expected = service.findById("test");

        assertThat(expected).isInstanceOf(Schema.class);
        assertThat(expected).hasFieldOrPropertyWithValue("name", "Updated");
    }

    @Test
    @Transactional
    public void it_should_delete_schema() throws Exception {
        // given a schema that exists
        service.create(new Schema("test", "Original"));
        assertThat(service.findById("test")).isNotNull();

        // when DELETE to /{id}
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.delete("/api/resources/schemas/test")
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andDo(print())
                .andExpect(status().isAccepted());

        // schema is now gone
        assertThat(service.findById("test")).isNull();
    }

    // TODO test authentication
}