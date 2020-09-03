package au.edu.ardc.registry.common.entity;

import au.edu.ardc.registry.TestHelper;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
class IdentifierTest {

	@Autowired
	TestEntityManager entityManager;

	@Test
	void auto_generated_uuid_test() {
		Record record = new Record();
		entityManager.persistAndFlush(record);
		Identifier identifier = TestHelper.mockIdentifier(record);
		entityManager.persistAndFlush(identifier);

		// uuid is generated and is the correct format
		assertThat(identifier.getId()).isNotNull();
		assertThat(identifier.getId()).isInstanceOf(UUID.class);
		assertThat(identifier.getId().toString()).matches("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})");
	}

	@Test
	void throws_exception_when_saving_without_a_record() {
		Identifier identifier = new Identifier();
		Assert.assertThrows(javax.persistence.PersistenceException.class, () -> {
			entityManager.persistAndFlush(identifier);
		});
	}

	@Test
	void an_identifier_must_have_a_date() {
		Identifier identifier = TestHelper.mockIdentifier();
		assertThat(identifier.getCreatedAt()).isInstanceOf(Date.class);
	}

	@Test
	void an_identifier_must_have_a_value() {
		String expected = "10.7531/XXAA998";
		Identifier identifier = TestHelper.mockIdentifier();
		identifier.setValue(expected);
		String actual = identifier.getValue();
		assertThat(expected).isEqualTo(actual);
	}

	@Test
	void an_identifier_must_set_dates() {
		Date expected = new Date();
		Identifier identifier = TestHelper.mockIdentifier();
		identifier.setUpdatedAt(expected);
		Date actual = identifier.getUpdatedAt();
		assertThat(expected).isEqualTo(actual);
		assertThat(identifier.getUpdatedAt()).isInstanceOf(Date.class);
	}

	@Test
	void an_identifier_must_set_type() {
		Identifier.Type expected = Identifier.Type.IGSN;
		Identifier identifier = TestHelper.mockIdentifier();
		identifier.setType(expected);
		Identifier.Type actual = identifier.getType();
		assertThat(expected).isEqualTo(actual);
	}

}
