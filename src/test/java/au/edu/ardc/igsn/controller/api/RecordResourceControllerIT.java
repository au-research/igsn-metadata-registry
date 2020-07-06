package au.edu.ardc.igsn.controller.api;

import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.service.KeycloakService;
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
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.UUID;

import static au.edu.ardc.igsn.TestHelper.mockRecord;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class RecordResourceControllerIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    RecordService service;

    /**
     * Still have to mock the kcService
     * because the http testing still can't access keycloak yet
     */
    @MockBean
    KeycloakService kcService;

    @Test
    @Transactional
    public void it_should_return_all_owned_records_when_get() throws Exception {

        // given a creator with 2 records
        UUID creatorID = UUID.randomUUID();
        UUID allocationID = UUID.randomUUID();

        for (int i=0;i<2;i++) {
            Record record = new Record(UUID.randomUUID());
            record.setCreatorID(creatorID);
            service.create(creatorID, allocationID, Record.OwnerType.User, null);
        }

        when(kcService.getUserUUID(any(HttpServletRequest.class))).thenReturn(creatorID);

        // when GET /
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.get("/api/resources/records/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        // returns 2 records
        mockMvc.perform(request)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").exists());
    }

    // todo get_/_pagination

    @Test
    @Transactional
    public void it_should_return_a_record_when_get_by_id() throws Exception {
        // given a record
        Record record = mockRecord();

        // when saved, it has a different UUID now
        Record saved = service.create(record.getCreatorID(), record.getAllocationID(), Record.OwnerType.User, null);

        // when get by id
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.get("/api/resources/records/" + saved.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        // it's the same record
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.creatorID").value(record.getCreatorID().toString()))
        ;
    }

    @Test
    @Transactional
    public void it_should_return_404_when_get_by_id_non_existence_record() throws Exception {
        // given a non existence record
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.get("/api/resources/records/" + UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        // it's 404
        mockMvc.perform(request)
                .andExpect(status().isNotFound());
    }
}