package au.edu.ardc.registry.igsn.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
class IGSNRecordTest {

	@Autowired
	TestEntityManager entityManager;

	@Test
	void IGSNRecordHasIGSNType() {
		IGSNRecord record = new IGSNRecord();
		entityManager.persistAndFlush(record);
		entityManager.refresh(record);

		// type is IGSN
		assertThat(record.getType()).isEqualTo("IGSN");
	}

}