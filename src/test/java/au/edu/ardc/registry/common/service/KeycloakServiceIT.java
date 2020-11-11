package au.edu.ardc.registry.common.service;

import au.edu.ardc.registry.KeycloakIntegrationTest;
import au.edu.ardc.registry.common.model.DataCenter;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class KeycloakServiceIT extends KeycloakIntegrationTest {

    @Autowired
    private KeycloakService keycloakService;

    @Test
    void getDataCenterByUUID() throws Exception {
        DataCenter dataCenter = keycloakService.getDataCenterByUUID(UUID.fromString("bfcefcfd-dc5c-4083-a554-85888334f353"));
        Assert.assertEquals("TestUserGroup1", dataCenter.getName());
    }

}