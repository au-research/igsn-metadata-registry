package au.edu.ardc.registry.igsn.validator;

import au.edu.ardc.registry.TestHelper;
import au.edu.ardc.registry.common.model.Allocation;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.igsn.model.IGSNAllocation;
import com.sun.xml.bind.v2.runtime.unmarshaller.XsiNilLoader;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class UserAccessValidationTest {


    @Test
    public void testUserAccessToIdentifier(){
        String identifier = "20.500.11812/XXAASSSSIIIIUUUU";
        User user = TestHelper.mockUser();
        user.setAllocations(Arrays.asList(TestHelper.mockIGSNAllocation()));
        for(Allocation allocation: user.getAllocations()){
            if(allocation.getType().equals("urn:ardc:igsn:allocation")){
                String prefix = ((IGSNAllocation) allocation).getPrefix();
                assertThat(prefix.equals("20.500.11812")).isTrue();
                String namespace = ((IGSNAllocation) allocation).getNamespace();
                assertThat(namespace.equals("XXAA")).isTrue();
                assertThat(identifier.startsWith(prefix + "/" + namespace)).isTrue();
            }
        }
    }
}
