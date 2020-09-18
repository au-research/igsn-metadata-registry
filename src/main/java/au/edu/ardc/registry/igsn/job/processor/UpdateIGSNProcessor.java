package au.edu.ardc.registry.igsn.job.processor;

import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.repository.IdentifierRepository;
import au.edu.ardc.registry.common.repository.VersionRepository;
import au.edu.ardc.registry.common.service.*;
import au.edu.ardc.registry.common.transform.TransformerFactory;
import au.edu.ardc.registry.exception.NotFoundException;
import au.edu.ardc.registry.exception.TransformerNotFoundException;
import au.edu.ardc.registry.igsn.model.IGSNAllocation;
import au.edu.ardc.registry.igsn.transform.ardcv1.ARDCv1ToRegistrationMetadataTransformer;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;

import java.time.Instant;
import java.util.List;

public class UpdateIGSNProcessor implements ItemProcessor<String, String> {

	private SchemaService schemaService;

	private KeycloakService kcService;

	private IdentifierRepository identifierRepository;

	private RecordService recordService;

	private VersionService versionService;

	private VersionRepository versionRepository;

	private String supportedSchema = SchemaService.ARDCv1;

	private Version existingRegistrationMDVersion;

	private IGSNAllocation allocation;

	private String landingPage;

	public UpdateIGSNProcessor(SchemaService schemaService, KeycloakService kcService,
			IdentifierRepository identifierRepository, RecordService recordService, VersionService versionService,
			VersionRepository versionRepository) {
		this.schemaService = schemaService;
		this.kcService = kcService;
		this.identifierRepository = identifierRepository;
		this.recordService = recordService;
		this.versionService = versionService;
		this.versionRepository = versionRepository;
	}

	@Override
	public String process(@NotNull String identifierValue) {
		String result = "";
		Identifier identifier = identifierRepository.findFirstByValueAndType(identifierValue, Identifier.Type.IGSN);
		Version newVersion = getRegistrationMetadata(identifier);
		if (isDifferentRegistrationMetadata(newVersion)) {
			// TODO replace current registration Metadata
			result = String.valueOf(updateMDS(newVersion));
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

	private int updateMDS(Version regMetaVersion) {
		int responseCode = 0;

		return responseCode;
	}

}
