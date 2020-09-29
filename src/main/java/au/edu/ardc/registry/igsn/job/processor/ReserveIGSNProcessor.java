package au.edu.ardc.registry.igsn.job.processor;

import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.model.Attribute;
import au.edu.ardc.registry.common.repository.IdentifierRepository;
import au.edu.ardc.registry.common.repository.RecordRepository;
import au.edu.ardc.registry.common.service.RequestService;
import au.edu.ardc.registry.igsn.service.IGSNRecordService;
import au.edu.ardc.registry.igsn.service.IGSNRequestService;
import org.apache.logging.log4j.core.Logger;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.w3c.dom.Attr;

import java.util.Date;
import java.util.UUID;

public class ReserveIGSNProcessor implements ItemProcessor<String, String> {

	RecordRepository recordRepository;

	IdentifierRepository identifierRepository;

	IGSNRequestService igsnService;

	private Request request;

	private ExecutionContext executionContext;

	private final RequestService requestService;

	private Logger logger;

	public ReserveIGSNProcessor(RecordRepository recordRepository, IdentifierRepository identifierRepository,
			IGSNRequestService igsnService, RequestService requestService) {
		this.recordRepository = recordRepository;
		this.identifierRepository = identifierRepository;
		this.igsnService = igsnService;
		this.requestService = requestService;
	}

	@BeforeStep
	public void beforeStep(final StepExecution stepExecution) {
		JobParameters jobParameters = stepExecution.getJobParameters();
		this.executionContext = stepExecution.getExecutionContext();
		String IGSNServiceRequestID = jobParameters.getString("IGSNServiceRequestID");
		this.request = igsnService.findById(IGSNServiceRequestID);
		this.logger = requestService.getLoggerFor(request);
	}

	@AfterStep
	public void afterStep(StepExecution stepExecution) {
		// todo store as metadata in IGSNServiceRequest
		logger.info("Processed: {}", stepExecution.getExecutionContext().getInt("importedRecords", 0));
		logger.info("Existed: {}", stepExecution.getExecutionContext().getInt("existedRecords", 0));
	}

	@Override
	public String process(String identifierValue) {

		String ownerID = this.request.getAttribute(Attribute.OWNER_ID);
		String ownerType = this.request.getAttribute(Attribute.OWNER_TYPE);
		String allocationID = this.request.getAttribute(Attribute.ALLOCATION_ID);
		String creatorID = this.request.getAttribute(Attribute.CREATOR_ID);

		if (identifierRepository.existsByTypeAndValue(Identifier.Type.IGSN, identifierValue)) {
			logger.warn("Identifier {} of type {} already exists", identifierValue, Identifier.Type.IGSN);
			this.executionContext.putInt("existedRecords", this.executionContext.getInt("existedRecords", 0) + 1);
			return null;
		}

		// create the record
		Record record = IGSNRecordService.create();
		record.setCreatedAt(new Date());
		record.setOwnerID(UUID.fromString(ownerID));
		record.setOwnerType(Record.OwnerType.valueOf(ownerType));
		record.setVisible(false);
		record.setAllocationID(UUID.fromString(allocationID));
		record.setCreatorID(UUID.fromString(creatorID));
		record.setRequestID(request.getId());

		record = recordRepository.saveAndFlush(record);
		logger.info("Created record {} ", record.getId());

		// create the identifier
		Identifier identifier = new Identifier();
		identifier.setCreatedAt(new Date());
		identifier.setUpdatedAt(new Date());
		identifier.setRecord(record);
		identifier.setType(Identifier.Type.IGSN);
		identifier.setValue(identifierValue);
		identifier.setStatus(Identifier.Status.RESERVED);

		identifier = identifierRepository.saveAndFlush(identifier);
		logger.info("Reserved identifier {} with type {} and value {}", identifier.getId(), identifier.getType(),
				identifier.getValue());

		this.executionContext.putInt("importedRecords", this.executionContext.getInt("importedRecords", 0) + 1);

		return identifier.getId().toString();
	}

}
