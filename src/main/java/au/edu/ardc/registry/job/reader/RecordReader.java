package au.edu.ardc.registry.job.reader;

import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.repository.RecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Component
@StepScope
public class RecordReader extends RepositoryItemReader<Record> {

	private static final Logger logger = LoggerFactory.getLogger(RecordReader.class);

	private final RecordRepository recordRepository;

	public RecordReader(RecordRepository recordRepository) {
		super();
		this.recordRepository = recordRepository;
		this.init();
	}

	@BeforeStep
	public void beforeStep(StepExecution stepExecution) {
		JobParameters jobParameters = stepExecution.getJobParameters();

		String method = jobParameters.getString("method") != null ? jobParameters.getString("method") : "findAll";
		String id = jobParameters.getString("id");
		this.setMethodName(method);
		if (id != null) {
			List<Object> list = new ArrayList<>();
			list.add(UUID.fromString(id));
			this.setArguments(list);
		}
		logger.debug("Setting method: {}, id: {}", method, id);
	}

	public void init() {
		this.setRepository(recordRepository);
		HashMap<String, Sort.Direction> sorts = new HashMap<>();
		sorts.put("id", Sort.Direction.DESC);
		this.setSort(sorts);
	}

}
