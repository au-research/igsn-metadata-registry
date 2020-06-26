package au.edu.ardc.igsn.entity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
public class SchemaTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test(expected = javax.persistence.PersistenceException.class)
    public void throws_exception_when_saving_a_schema_without_id() {
        // given a blank schema, when saved, expects a persistence exception
        Schema schema = new Schema();
        entityManager.persistAndFlush(schema);
    }

    @Test
    public void a_schema_can_be_saved_with_id_and_name() {
        // given a schema, saving without exception
        Schema schema = new Schema("csiro-v3-igsn", "CSIRO version 3");
        entityManager.persistAndFlush(schema);
    }

    // a schema has unique id
    @Test(expected = javax.persistence.EntityExistsException.class)
    public void a_schema_has_unique_id() {
        // given a schema
        Schema schema = new Schema("csiro-v3-igsn", "CSIRO version 3");
        entityManager.persistAndFlush(schema);

        // when save another 1 with the exact same id, expects exception
        Schema schema2 = new Schema("csiro-v3-igsn", "CSIRO version 3 duplicate");
        entityManager.persistAndFlush(schema2);
    }

}