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
import au.edu.ardc.registry.igsn.service.IGSNVersionService;
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

	private IdentifierService identifierService;

	private RecordService recordService;

	private IGSNVersionService igsnVersionService;

	private URLService urlService;

	private String supportedSchema = SchemaService.ARDCv1;

	private Version existingRegistrationMDVersion;

	public UpdateIGSNProcessor(SchemaService schemaService, KeycloakService kcService,
			IdentifierService identifierService, RecordService recordService, IGSNVersionService igsnVersionService,
			URLService urlService) {
		this.schemaService = schemaService;
		this.kcService = kcService;
		this.identifierService = identifierService;
		this.recordService = recordService;
		this.igsnVersionService = igsnVersionService;
		this.urlService = urlService;

	}

	@Override
	public String process(@NotNull String identifierValue) {
		String result = "";
		Identifier identifier = identifierService.findByValueAndType(identifierValue, Identifier.Type.IGSN);
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
