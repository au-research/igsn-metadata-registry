package au.edu.ardc.igsn.entity;


import au.edu.ardc.igsn.TestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
public class IdentifierTest {

    @Autowired
    TestEntityManager entityManager;


    @Test
    public void an_identifier_should_have_auto_generated_uuid() {
        Identifier identifier = TestHelper.mockIdentifier();

        // uuid is generated and is the correct format
        assertThat(identifier.getId()).isNotNull();
        assertThat(identifier.getId()).isInstanceOf(UUID.class);
        assertThat(identifier.getId().toString()).matches("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})");
    }

    @Test
    public void an_identifier_must_have_a_record() {
        Identifier identifier = TestHelper.mockIdentifier();
        assertThat(identifier.getRecord()).isInstanceOf(Record.class);
    }

    @Test
    public void an_identifier_must_have_a_date() {
        Identifier identifier = TestHelper.mockIdentifier();
        assertThat(identifier.getCreatedAt()).isInstanceOf(Date.class);
    }

    @Test
    public void an_identifier_must_have_a_value() {
        String expected = "10.7531/XXAA998";
        Identifier identifier = TestHelper.mockIdentifier();
        identifier.setValue(expected);
        String actual = identifier.getValue();
        assertThat(expected).isEqualTo(actual);
    }

    @Test
    public void an_identifier_must_set_dates() {
        Date expected = new Date();
        Identifier identifier = TestHelper.mockIdentifier();
        identifier.setUpdatedAt(expected);
        Date actual = identifier.getUpdatedAt();
        assertThat(expected).isEqualTo(actual);
        assertThat(identifier.getUpdatedAt()).isInstanceOf(Date.class);
    }

    @Test
    public void an_identifier_must_set_type() {
        Identifier.Type expected = Identifier.Type.IGSN;
        Identifier identifier = TestHelper.mockIdentifier();
        identifier.setType(expected);
        Identifier.Type actual = identifier.getType();
        assertThat(expected).isEqualTo(actual);
    }

}
