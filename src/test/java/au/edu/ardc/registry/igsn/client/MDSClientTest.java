package au.edu.ardc.registry.igsn.client;

import au.edu.ardc.registry.TestHelper;
import au.edu.ardc.registry.exception.MDSClientConfigurationException;
import au.edu.ardc.registry.exception.MDSClientException;
import au.edu.ardc.registry.igsn.model.IGSNAllocation;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MDSClientTest {

    @Test
    void configurationException()
    {

        IGSNAllocation ia = TestHelper.mockIGSNAllocation();
        ia.setName(null);
        ia.setMds_password(null);
        Assert.assertThrows(MDSClientConfigurationException.class, () -> {
            MDSClient mc = new MDSClient(ia);
        });
        try{
            MDSClient mc = new MDSClient(ia);
        }catch(Exception e){
            String msg = e.getMessage();
            System.out.print(msg);
        }
    }


}