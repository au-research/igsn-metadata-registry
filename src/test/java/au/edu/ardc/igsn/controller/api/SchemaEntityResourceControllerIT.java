package au.edu.ardc.igsn.controller.api;

import au.edu.ardc.igsn.TestHelper;
import au.edu.ardc.igsn.entity.SchemaEntity;
import au.edu.ardc.igsn.service.SchemaEntityService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.KeycloakPrincipal;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static au.edu.ardc.igsn.TestHelper.asJsonString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class SchemaEntityResourceControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private SchemaEntityService service;

//    @Before
//    public void setup() {
//        mockMvc = MockMvcBuilders
//                .webAppContextSetup(context)
//                .apply(springSecurity())
//                .build();
//    }

    @Test
    public void it_should_show_all_schemas() throws Exception {
        // given 2 schemas
        service.create(new SchemaEntity("first", "First"));
        service.create(new SchemaEntity("second", "Second"));

        // with this user
        KeycloakPrincipal mockPrincipal = Mockito.mock(KeycloakPrincipal.class);
        Mockito.when(mockPrincipal.getName()).thenReturn("dude");

        // when GET /api/resources/schemas/, see that they're there
        mockMvc.perform(get("/api/resources/schemas/"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").isNotEmpty());
    }

    @Test
    @Transactional
    public void it_should_show_one_schema() throws Exception {
        // given a schema
        service.create(new SchemaEntity("test", "Show"));

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
//                        .with(httpBasic("user","password"))
//                        .header("Authorization", "Basic dXNlcjpwYXNzd29yZA==")
                        .content(asJsonString(new SchemaEntity("test", "Test")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());

        // schema `test` now exist
        assertThat(service.findById("test")).isNotNull();
        assertThat(service.findById("test")).isInstanceOf(SchemaEntity.class);
    }

    @Test
    @Transactional
    public void it_should_show_validation_error() throws Exception {
        assertThat(service.findById("test")).isNull();

        SchemaEntity invalidSchema = new SchemaEntity();
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
        service.create(new SchemaEntity("test", "Original"));

        // when PUT to /{id}
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.put("/api/resources/schemas/test")
                        .content(TestHelper.asJsonString(new SchemaEntity("test", "Updated")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
//                .andDo(print())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Updated"));

        // schema now has updated name
        SchemaEntity expected = service.findById("test");

        assertThat(expected).isInstanceOf(SchemaEntity.class);
        assertThat(expected).hasFieldOrPropertyWithValue("name", "Updated");
    }

    @Test
    @Transactional
    public void it_should_delete_schema() throws Exception {
        // given a schema that exists
        service.create(new SchemaEntity("test", "Original"));
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