package au.edu.ardc.registry.igsn.job.processor;

import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.repository.IdentifierRepository;
import au.edu.ardc.registry.common.repository.RecordRepository;
import au.edu.ardc.registry.igsn.service.IGSNRequestService;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;

import java.util.Date;
import java.util.UUID;

public class ReserveIGSNProcessor implements ItemProcessor<String, String> {

	RecordRepository recordRepository;

	IdentifierRepository identifierRepository;

	IGSNRequestService igsnService;

	private String allocationID;

	private String creatorID;

	private String ownerID;

	private String ownerType;

	private Request request;

	private ExecutionContext executionContext;

	public ReserveIGSNProcessor(RecordRepository recordRepository, IdentifierRepository identifierRepository,
								IGSNRequestService igsnService) {
		this.recordRepository = recordRepository;
		this.identifierRepository = identifierRepository;
		this.igsnService = igsnService;
	}

	@BeforeStep
	public void beforeStep(final StepExecution stepExecution) {
		JobParameters jobParameters = stepExecution.getJobParameters();
		this.executionContext = stepExecution.getExecutionContext();
		this.ownerID = jobParameters.getString("ownerID");
		this.ownerType = jobParameters.getString("ownerType");
		this.creatorID = jobParameters.getString("creatorID");
		this.allocationID = jobParameters.getString("allocationID");
		String IGSNServiceRequestID = jobParameters.getString("IGSNServiceRequestID");
		this.request = igsnService.findById(IGSNServiceRequestID);
	}

	@AfterStep
	public void afterStep(StepExecution stepExecution) {
		// todo store as metadata in IGSNServiceRequest
		igsnService.getLoggerFor(request)
				.info(String.format("Processed: %s", stepExecution.getExecutionContext().getInt("importedRecords", 0)));
		igsnService.getLoggerFor(request)
				.info(String.format("Existed: %s", stepExecution.getExecutionContext().getInt("existedRecords", 0)));
	}

	@Override
	public String process(String identifierValue) {

		if (identifierRepository.existsByTypeAndValue(Identifier.Type.IGSN, identifierValue)) {
			igsnService.getLoggerFor(request).warning(
					String.format("Identifier %s of type %s already exists", identifierValue, Identifier.Type.IGSN));
			this.executionContext.putInt("existedRecords", this.executionContext.getInt("existedRecords", 0) + 1);
			return null;
		}

		// create the record
		Record record = new Record();
		record.setCreatedAt(new Date());
		record.setOwnerID(UUID.fromString(ownerID));
		record.setOwnerType(Record.OwnerType.valueOf(ownerType));
		record.setVisible(false);
		record.setAllocationID(UUID.fromString(allocationID));
		record.setCreatorID(UUID.fromString(creatorID));
		record.setRequestID(request.getId());

		record = recordRepository.saveAndFlush(record);
		igsnService.getLoggerFor(request).info(String.format("Created record %s ", record.getId()));

		// create the identifier
		Identifier identifier = new Identifier();
		identifier.setCreatedAt(new Date());
		identifier.setRecord(record);
		identifier.setType(Identifier.Type.IGSN);
		identifier.setValue(identifierValue);
		identifier.setStatus(Identifier.Status.RESERVED);

		identifier = identifierRepository.saveAndFlush(identifier);
		igsnService.getLoggerFor(request).info(String.format("Reserved identifier %s with type %s and value %s",
				identifier.getId(), identifier.getType(), identifier.getValue()));

		this.executionContext.putInt("importedRecords", this.executionContext.getInt("importedRecords", 0) + 1);

		return identifier.getId().toString();
	}

}
