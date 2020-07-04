package au.edu.ardc.igsn.controller.api;

import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.entity.SchemaEntity;
import au.edu.ardc.igsn.service.RecordService;
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
import java.util.UUID;

import static au.edu.ardc.igsn.TestHelper.asJsonString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class RecordResourceControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    RecordService service;

    @Test
    public void it_should_return_all_records_when_get() throws Exception {
        ArrayList<Record> expected = new ArrayList<>();
        for (int i=0;i<2;i++) {
            expected.add(new Record(UUID.randomUUID()));
        }
        when(service.findAll()).thenReturn(expected);

        // given 2 records
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.get("/api/resources/records/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").exists());
    }

    // todo get_/_with_pagination

    @Test
    public void it_should_return_a_record_when_get_by_id() {
        Record expected = new Record();

    }
}