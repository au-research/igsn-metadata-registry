package au.edu.ardc.registry.igsn.job.processor;

import au.edu.ardc.registry.igsn.entity.IGSNServiceRequest;
import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.repository.IdentifierRepository;
import au.edu.ardc.registry.common.repository.RecordRepository;
import au.edu.ardc.registry.igsn.service.IGSNService;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;

import java.util.UUID;

public class TransferIGSNProcessor implements ItemProcessor<String, String> {

	IGSNService igsnService;

	RecordRepository recordRepository;

	IdentifierRepository identifierRepository;

	private String ownerID;

	private String ownerType;

	private IGSNServiceRequest request;

	public TransferIGSNProcessor(RecordRepository recordRepository, IdentifierRepository identifierRepository,
			IGSNService igsnService) {
		this.recordRepository = recordRepository;
		this.identifierRepository = identifierRepository;
		this.igsnService = igsnService;
	}

	@BeforeStep
	public void beforeStep(final StepExecution stepExecution) {
		JobParameters jobParameters = stepExecution.getJobParameters();
		this.ownerID = jobParameters.getString("ownerID");
		this.ownerType = jobParameters.getString("ownerType");
		String IGSNServiceRequestID = jobParameters.getString("IGSNServiceRequestID");
		this.request = igsnService.findById(IGSNServiceRequestID);
	}

	@Override
	public String process(String identifierValue) {
		if (identifierRepository.existsByTypeAndValue(Identifier.Type.IGSN, identifierValue)) {
			igsnService.getLoggerFor(request).severe(
					String.format("Identifier %s of type %s does not exists", identifierValue, Identifier.Type.IGSN));
		}

		Identifier identifier = identifierRepository.findFirstByValueAndType(identifierValue, Identifier.Type.IGSN);
		Record record = identifier.getRecord();

		record.setOwnerType(Record.OwnerType.valueOf(ownerType));
		record.setOwnerID(UUID.fromString(ownerID));
		recordRepository.save(record);
		igsnService.getLoggerFor(request).info(String.format("Updated record %s ownerType to %s and ownerID to %s",
				record.getId(), ownerType, ownerID));

		return identifierValue;
	}

}
