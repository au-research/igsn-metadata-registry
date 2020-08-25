package au.edu.ardc.igsn.repository;

import au.edu.ardc.igsn.entity.IGSNServiceRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@DataJpaTest
class IGSNServiceRequestRepositoryTest {

    @Autowired
    TestEntityManager entityManager;

    @Autowired
    IGSNServiceRequestRepository repository;

    @Test
    void findById() {
        IGSNServiceRequest request = new IGSNServiceRequest();
        repository.save(request);

        UUID id = request.getId();

        Optional<IGSNServiceRequest> dbFound = repository.findById(id);

        assertThat(dbFound.isPresent()).isTrue();
        IGSNServiceRequest found = dbFound.get();
        assertThat(found).isInstanceOf(IGSNServiceRequest.class);
        assertThat(found.getId()).isEqualTo(request.getId());
    }

    @Test
    void existsById() {
        assertThat(repository.existsById(UUID.randomUUID())).isFalse();

        IGSNServiceRequest request = new IGSNServiceRequest();
        repository.save(request);
        UUID id = request.getId();

        assertThat(repository.existsById(id)).isTrue();
    }
}