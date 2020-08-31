package au.edu.ardc.registry.igsn.entity;

import au.edu.ardc.registry.igsn.entity.IGSNServiceRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
class IGSNServiceRequestTest {
    @Autowired
    TestEntityManager entityManager;

    @Test
    void auto_generated_uuid_test() {
        IGSNServiceRequest request = new IGSNServiceRequest();
        entityManager.persistAndFlush(request);

        // uuid is generated and is the correct format
        assertThat(request.getId()).isNotNull();
        assertThat(request.getId()).isInstanceOf(UUID.class);
        assertThat(request.getId().toString()).matches("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})");
    }
}