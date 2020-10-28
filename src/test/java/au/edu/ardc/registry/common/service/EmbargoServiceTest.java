package au.edu.ardc.registry.common.service;

import au.edu.ardc.registry.TestHelper;
import au.edu.ardc.registry.common.entity.Embargo;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.repository.EmbargoRepository;
import au.edu.ardc.registry.common.repository.RecordRepository;
import au.edu.ardc.registry.common.util.Helpers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { EmbargoService.class })
public class EmbargoServiceTest {

	@Autowired
	private EmbargoService embargoService;

	@MockBean
	private EmbargoRepository embargoRepository;

	@MockBean
	private RecordRepository recordRepository;

	@Test
	void findAllEmbargoEnd() {

		Date embargoEnd = Helpers.convertDate("2020-10-27");

		for (int i = 0; i < 110; i++) {
			Record record = TestHelper.mockRecord();
			recordRepository.save(record);

			Embargo embargo = TestHelper.mockEmbargo(record);

			embargo.setEmbargoEnd(embargoEnd);
			embargo.setRecord(record);

			embargoRepository.save(embargo);

			when(embargoRepository.findById(any(UUID.class))).thenReturn(Optional.of(embargo));
		}

		List<Embargo> embargos = embargoService.findAllEmbargoToEnd();

		System.out.println(embargos.size() + " is the size");
	}

}
