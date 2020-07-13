package au.edu.ardc.igsn.controller.api;

import au.edu.ardc.igsn.TestHelper;
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

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import static au.edu.ardc.igsn.TestHelper.asJsonString;
import static au.edu.ardc.igsn.TestHelper.mockRecord;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @MockBean
    KeycloakService kcService;

    @Test
    public void it_should_return_all_records_when_get() throws Exception {

        // given a creator with 2 records
        UUID creatorID = UUID.randomUUID();

        ArrayList<Record> expected = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Record record = TestHelper.mockRecord();
            record.setCreatorID(creatorID);
            expected.add(record);
        }
        when(service.findOwned(any())).thenReturn(expected);

        // probably not need this here, because we already mock out the service response
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

    // todo get_/_with_pagination

    @Test
    public void it_should_return_a_record_when_get_by_id() throws Exception {
        Record record = new Record(UUID.randomUUID());
        when(service.findById(record.getId().toString())).thenReturn(record);

        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.get("/api/resources/records/" + record.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(record.getId().toString()));
    }

    @Test
    public void it_should_store_record_with_creator_owner_by_default() throws Exception {

        Record record = TestHelper.mockRecord(UUID.randomUUID());
        UUID creatorID = record.getCreatorID();
        UUID allocationID = record.getAllocationID();
        UUID datacenterID = record.getDataCenterID();

        // given a creator with an allocation and a proposed datacenter
        when(kcService.getUserUUID(any(HttpServletRequest.class))).thenReturn(creatorID);
        when(service.create(creatorID, allocationID, Record.OwnerType.User, null))
                .thenReturn(record);

        // when POST to the records endpoint with the allocationID and datacenterID
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.post("/api/resources/records/")
                        .param("allocationID", allocationID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(record.getId().toString()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.allocationID").exists())
                .andExpect(jsonPath("$.allocationID").value(allocationID.toString()))
                .andExpect(jsonPath("$.ownerID").value(creatorID.toString()))
                .andExpect(jsonPath("$.ownerType").value(Record.OwnerType.User.toString()))
        ;
    }

    @Test
    public void it_should_store_the_datacenter_param_when_ownerType_is_set() throws Exception {
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        record.setOwnerType(Record.OwnerType.DataCenter);
        UUID creatorID = record.getCreatorID();
        UUID allocationID = record.getAllocationID();
        UUID datacenterID = record.getDataCenterID();

        when(kcService.getUserUUID(any(HttpServletRequest.class))).thenReturn(creatorID);
        when(service.create(creatorID, allocationID, Record.OwnerType.DataCenter, datacenterID))
                .thenReturn(record);

        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.post("/api/resources/records/")
                        .param("allocationID", allocationID.toString())
                        .param("ownerType", "DataCenter")
                        .param("datacenterID", datacenterID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(record.getId().toString()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.allocationID").exists())
                .andExpect(jsonPath("$.allocationID").value(allocationID.toString()))
                .andExpect(jsonPath("$.ownerID").value(creatorID.toString()))
                .andExpect(jsonPath("$.ownerType").value(Record.OwnerType.DataCenter.toString()))
        ;
    }

    @Test
    public void it_should_404_when_updating_a_nonexistent_record() throws Exception {
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.put("/api/resources/records/" + UUID.randomUUID().toString())
                        .content(asJsonString(TestHelper.mockRecord()))
                        .param("allocationID", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        // it should be ok and the data be updated
        mockMvc.perform(request)
                .andExpect(status().isNotFound());
    }

    @Test
    public void it_should_update_record_when_put() throws Exception {
        Record record = TestHelper.mockRecord(UUID.randomUUID());
        UUID creatorID = record.getCreatorID();
        UUID allocationID = record.getAllocationID();
        UUID datacenterID = record.getDataCenterID();
        record.setModifiedAt(new SimpleDateFormat("yyyy/MM/dd").parse("2000/02/002"));

        Date updatedDate = new Date();
        Record updatedRecord = record;
        updatedRecord.setModifiedAt(updatedDate);

        // given a creator with an allocation and a proposed datacenter
        when(kcService.getUserUUID(any(HttpServletRequest.class))).thenReturn(creatorID);
        when(service.exists(record.getId().toString())).thenReturn(true);
        when(service.update(any(Record.class), eq(creatorID))).thenReturn(updatedRecord);

        // when PUT to the record endpoint
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.put("/api/resources/records/" + record.getId().toString())
                        .content(asJsonString(updatedRecord))
                        .param("allocationID", allocationID.toString())
                        .param("datacenterID", datacenterID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        // it should be ok and the data be updated
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(record.getId().toString()));
    }

    @Test
    public void it_should_404_when_deleting_a_nonexistent_record() throws Exception {
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.delete("/api/resources/records/" + UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        // it should be ok and the data be updated
        mockMvc.perform(request)
                .andExpect(status().isNotFound());
    }

    @Test
    public void it_should_delete_a_record() throws Exception {
        // given a record
        Record record = new Record(UUID.randomUUID());
        when(service.exists(record.getId().toString())).thenReturn(true);
        when(service.findById(record.getId().toString())).thenReturn(record);
        when(service.delete(record)).thenReturn(true);

        // when delete
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.delete("/api/resources/records/" + record.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        // it should be accepted
        mockMvc.perform(request)
                .andExpect(status().isAccepted());
    }
}