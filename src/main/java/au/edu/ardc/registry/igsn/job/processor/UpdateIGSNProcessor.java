package au.edu.ardc.registry.igsn.job.processor;

import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.model.schema.XMLSchema;
import au.edu.ardc.registry.common.repository.IdentifierRepository;
import au.edu.ardc.registry.common.repository.RecordRepository;
import au.edu.ardc.registry.common.service.*;
import au.edu.ardc.registry.common.transform.Transformer;
import au.edu.ardc.registry.common.transform.TransformerFactory;
import au.edu.ardc.registry.exception.NotFoundException;
import au.edu.ardc.registry.exception.TransformerNotFoundException;
import au.edu.ardc.registry.igsn.service.IGSNService;
import au.edu.ardc.registry.igsn.transform.ardcv1.ARDCv1ToRegistrationMetadataTransformer;
import au.edu.ardc.registry.igsn.validator.VersionContentValidator;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class UpdateIGSNProcessor implements ItemProcessor<String, String> {

	private SchemaService schemaService;

	private KeycloakService kcService;

	private IdentifierRepository identifierRepository;

	private RecordService recordService;

	private VersionService versionService;

	private IdentifierService identifierService;

	private String supportedSchema = SchemaService.ARDCv1;

	private Version existingRegistrationMDVersion;

	public UpdateIGSNProcessor(SchemaService schemaService, KeycloakService kcService,
			IdentifierRepository identifierRepository, RecordService recordService, VersionService versionService,
			IdentifierService identifierService) {
		this.schemaService = schemaService;
		this.kcService = kcService;
		this.identifierRepository = identifierRepository;
		this.recordService = recordService;
		this.versionService = versionService;
		this.identifierService = identifierService;
	}

	@Override
	public String process(@NotNull String identifierValue) {
		String result = "";
		Identifier identifier = identifierRepository.findByValueAndType(identifierValue, Identifier.Type.IGSN);
		Version newVersion = getRegistrationMetadata(identifier);
		if (isDifferentRegistrationMetadata(newVersion)) {
			updateMDS(newVersion);
		}
		return result;
	}

	@BeforeStep
	public void beforeStep(final StepExecution stepExecution) {
		JobParameters jobParameters = stepExecution.getJobParameters();
		String IGSNServiceRequestID = jobParameters.getString("IGSNServiceRequestID");

	}

	private Version getRegistrationMetadata(Identifier identifier)
			throws TransformerNotFoundException, NotFoundException {
		List<Version> versions = identifier.getRecord().getCurrentVersions();
		ARDCv1ToRegistrationMetadataTransformer transformer = null;
		boolean hasSupportedVersion = false;
		existingRegistrationMDVersion = null;
		Version supportedVersion = null;
		for (Version version : versions) {
			if (version.getSchema().equals(SchemaService.IGSNREGv1)) {
				existingRegistrationMDVersion = version;
			}
			if (version.getSchema().equals(supportedSchema)) {
				hasSupportedVersion = true;
				supportedVersion = version;
				try {
					Schema fromSchema = schemaService.getSchemaByID(supportedSchema);
					Schema toSchema = schemaService.getSchemaByID(SchemaService.IGSNREGv1);
					transformer = (ARDCv1ToRegistrationMetadataTransformer) TransformerFactory.create(fromSchema,
							toSchema);
				}
				catch (TransformerNotFoundException e) {
					throw e;
				}
			}
		}
		if (!hasSupportedVersion) {
			throw new NotFoundException(
					"Unable to generate registration metadata missing " + supportedSchema + " version");
		}
		// TODO get some user info
		/**
		 * .setParam("registrantName") .setParam("nameIdentifier")
		 * .setParam("nameIdentifierScheme")
		 *
		 */
		String utcDateTimeStr = Instant.now().toString();
		transformer.setParam("eventType", "CREATED").setParam("timeStamp", utcDateTimeStr);
		return transformer.transform(supportedVersion);
	}

	private boolean isDifferentRegistrationMetadata(Version newVersion) {
		String newHash = VersionService.getHash(newVersion);
		String oldHash = VersionService.getHash(existingRegistrationMDVersion);
		return (!oldHash.equals(newHash));
	}

	private boolean updateMDS(Version regMetaVersion) {
		boolean success = false;

		return success;
	}

}
