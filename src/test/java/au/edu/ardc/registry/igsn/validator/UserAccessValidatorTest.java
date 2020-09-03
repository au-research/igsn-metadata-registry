package au.edu.ardc.registry.igsn.validator;

import au.edu.ardc.registry.KeycloakIntegrationTest;
import au.edu.ardc.registry.common.model.Allocation;
import au.edu.ardc.registry.common.service.KeycloakService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class UserAccessValidatorIT extends KeycloakIntegrationTest{


        @Autowired
    private KeycloakService kcService;

    @Test
    public void canUserEditPrefix() throws Exception {
        String identifier = "20.500.11812/XXAASSSSIIIIUUUU";
        String allocationId = "72dcf894-a6c2-4a31-904a-23148255b57f";
        Allocation a = kcService.getAllocationByResourceID(allocationId);
        Map<String, List<String>> attributes;
        attributes = a.getAttributes();
        if(attributes != null && !attributes.isEmpty()){
            for(Map.Entry<String,List<String>> entry : attributes.entrySet()){
                if(entry.getKey().equals("allocation"))
                    assertThat(entry.getValue().contains("20.500.11812/XXAA")).isTrue();
            }
        }

    }

}