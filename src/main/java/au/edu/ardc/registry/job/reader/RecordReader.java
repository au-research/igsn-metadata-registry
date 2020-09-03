package au.edu.ardc.registry.job.reader;

import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.repository.RecordRepository;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
@StepScope
public class RecordReader extends RepositoryItemReader<Record> {

	private final RecordRepository recordRepository;

	public RecordReader(RecordRepository recordRepository) {
		super();
		this.recordRepository = recordRepository;
		this.init();
	}

	@BeforeStep
	public void beforeStep(StepExecution stepExecution) {
		JobParameters jobParameters = stepExecution.getJobParameters();

		String method = jobParameters.getString("method");
		this.setMethodName(method);
	}

	public void init() {
		this.setRepository(recordRepository);
		// this.setMethodName("findAll");
		// List<Object> list = new ArrayList<>();
		// list.add(UUID.fromString("f7441613-c0db-4243-aeb5-4f0319577248"));
		// this.setArguments(list);
		// this.setPageSize(10);
		HashMap<String, Sort.Direction> sorts = new HashMap<>();
		sorts.put("id", Sort.Direction.DESC);
		this.setSort(sorts);
	}

}
