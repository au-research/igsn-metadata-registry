package au.edu.ardc.registry.igsn.job.processor;

import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.model.Attribute;
import au.edu.ardc.registry.common.repository.IdentifierRepository;
import au.edu.ardc.registry.common.repository.RecordRepository;
import au.edu.ardc.registry.common.service.RequestService;
import au.edu.ardc.registry.igsn.service.IGSNRequestService;
import org.apache.logging.log4j.core.Logger;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;

import java.util.UUID;

public class TransferIGSNProcessor implements ItemProcessor<String, String> {

	private final RequestService requestService;

	IGSNRequestService igsnService;

	RecordRepository recordRepository;

	IdentifierRepository identifierRepository;

	private Request request;

	private Logger logger;

	public TransferIGSNProcessor(RecordRepository recordRepository, IdentifierRepository identifierRepository,
			IGSNRequestService igsnService, RequestService requestService) {
		this.recordRepository = recordRepository;
		this.identifierRepository = identifierRepository;
		this.igsnService = igsnService;
		this.requestService = requestService;
	}

	@BeforeStep
	public void beforeStep(final StepExecution stepExecution) {
		JobParameters jobParameters = stepExecution.getJobParameters();
		String IGSNServiceRequestID = jobParameters.getString("IGSNServiceRequestID");
		this.request = igsnService.findById(IGSNServiceRequestID);
		this.logger = requestService.getLoggerFor(request);
	}

	@Override
	public String process(String identifierValue) {
		String ownerID = this.request.getAttribute(Attribute.OWNER_ID);
		String ownerType = this.request.getAttribute(Attribute.OWNER_TYPE);

		if (identifierRepository.existsByTypeAndValue(Identifier.Type.IGSN, identifierValue)) {
			logger.error("Identifier {} of type {} does not exists", identifierValue, Identifier.Type.IGSN);
		}

		Identifier identifier = identifierRepository.findFirstByValueAndType(identifierValue, Identifier.Type.IGSN);
		Record record = identifier.getRecord();

		record.setOwnerType(Record.OwnerType.valueOf(ownerType));
		record.setOwnerID(UUID.fromString(ownerID));
		recordRepository.save(record);
		logger.info("Updated record {} ownerType to {} and ownerID to {}", record.getId(), ownerType, ownerID);

		return identifierValue;
	}

}
