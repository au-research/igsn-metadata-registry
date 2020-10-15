package au.edu.ardc.registry.igsn.job.processor;

import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.common.model.Attribute;
import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.provider.LandingPageProvider;
import au.edu.ardc.registry.common.provider.Metadata;
import au.edu.ardc.registry.common.provider.MetadataProviderFactory;
import au.edu.ardc.registry.common.service.IdentifierService;
import au.edu.ardc.registry.common.service.KeycloakService;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.service.VersionService;
import au.edu.ardc.registry.common.transform.TransformerFactory;
import au.edu.ardc.registry.exception.NotFoundException;
import au.edu.ardc.registry.exception.TransformerNotFoundException;
import au.edu.ardc.registry.igsn.client.MDSClient;
import au.edu.ardc.registry.igsn.model.IGSNAllocation;
import au.edu.ardc.registry.igsn.service.IGSNRequestService;
import au.edu.ardc.registry.igsn.service.IGSNVersionService;
import au.edu.ardc.registry.igsn.transform.ardcv1.ARDCv1ToRegistrationMetadataTransformer;
import org.apache.logging.log4j.core.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public class UpdateIGSNProcessor implements ItemProcessor<String, String> {

	private final SchemaService schemaService;

	private final KeycloakService kcService;

	private final IdentifierService identifierService;

	private final IGSNVersionService igsnVersionService;

	private final IGSNRequestService igsnRequestService;

	private IGSNAllocation allocation;

	private UUID creatorID;

	private String landingPage;

	private Logger requestLog;

	public UpdateIGSNProcessor(SchemaService schemaService, KeycloakService kcService,
			IdentifierService identifierService, IGSNVersionService igsnVersionService,
			IGSNRequestService igsnRequestService) {
		this.schemaService = schemaService;
		this.kcService = kcService;
		this.identifierService = identifierService;
		this.igsnVersionService = igsnVersionService;
		this.igsnRequestService = igsnRequestService;
	}

	@BeforeStep
	public void beforeStep(final StepExecution stepExecution) throws Exception {
		JobParameters jobParameters = stepExecution.getJobParameters();
		String IGSNServiceRequestID = jobParameters.getString("IGSNServiceRequestID");
		Request request = igsnRequestService.findById(IGSNServiceRequestID);
		requestLog = igsnRequestService.getLoggerFor(request);

		requestLog.info("Started Update for Request: {}", IGSNServiceRequestID);
		request.setMessage("Updating IGSN Metadata");
		igsnRequestService.save(request);

		// obtain allocation details for use with minting
		creatorID = UUID.fromString(request.getAttribute(Attribute.CREATOR_ID));
		String allocationID = request.getAttribute(Attribute.ALLOCATION_ID);
		allocation = (IGSNAllocation) kcService.getAllocationByResourceID(allocationID);

		requestLog.debug("creatorID: {}", creatorID);
		requestLog.debug("allocationID: {}", allocationID);
	}

	@Override
	public String process(@NotNull String identifierValue) throws Exception {
		requestLog.debug("Started processing identifier: {}", identifierValue);

		// obtain Identifier
		Identifier identifier = identifierService.findByValueAndType(identifierValue, Identifier.Type.IGSN);
		requestLog.debug("Found identifier: {}", identifier.getId());

		byte[] registrationMetaBody = addRegistrationMetadata(identifier);
		requestLog.debug("obtained Registration Metadata body");

		mintIGSN(registrationMetaBody, identifierValue, landingPage);
		requestLog.debug("Updated Registration Metadata for {} and landingPage: {}", identifierValue, landingPage);

		requestLog.info("Finished Updating Metadata for Identifier {}", identifierValue);
		return "";
	}

	/**
	 * Generate the Registration Metadata and save it in a new Version for a given
	 * {@link Identifier}.
	 * @param identifier the {@link Identifier} to create {@link Version} out of
	 * @return a {@link String} Registration Metadata
	 * @throws TransformerNotFoundException when
	 * {@link au.edu.ardc.registry.common.transform.Transformer} is not found
	 * @throws NotFoundException when {@link Record} or {@link Version} is not Found
	 */
	private byte[] addRegistrationMetadata(Identifier identifier)
			throws TransformerNotFoundException, NotFoundException {

		// obtain record
		Record record = identifier.getRecord();

		// obtain version
		String supportedSchema = SchemaService.ARDCv1;
		Version supportedVersion = igsnVersionService.getCurrentVersionForRecord(record, supportedSchema);
		if (supportedVersion == null) {
			throw new NotFoundException(
					"Unable to generate registration metadata missing " + supportedSchema + " version");
		}

		Schema fromSchema = schemaService.getSchemaByID(supportedSchema);
		Schema toSchema = schemaService.getSchemaByID(SchemaService.IGSNREGv1);

		// obtain landing page
		LandingPageProvider landingPageProvider = (LandingPageProvider) MetadataProviderFactory.create(fromSchema,
				Metadata.LandingPage);
		landingPage = landingPageProvider.get(new String(supportedVersion.getContent()));

		// transform to registration metadata
		ARDCv1ToRegistrationMetadataTransformer transformer = (ARDCv1ToRegistrationMetadataTransformer) TransformerFactory
				.create(fromSchema, toSchema);
		String utcDateTimeStr = Instant.now().toString();
		transformer.setParam("eventType", "updated").setParam("timeStamp", utcDateTimeStr).setParam("registrantName",
				allocation.getMds_username());

		// store registration metadata in a new version
		Version registrationMetadataVersion = transformer.transform(supportedVersion);
		Version version = addVersion(registrationMetadataVersion, record);
		requestLog.debug("Created version {}", version.getId());

		return registrationMetadataVersion.getContent();
	}

	/**
	 * Helper method to persist a {@link Version}. Also end all current version of the
	 * same Schema
	 * @param version the {@link Version} to persist
	 * @param record the {@link Record} to link with
	 * @return the persisted {@link Version}
	 */
	private Version addVersion(Version version, Record record) {
		// End current Version
		Version currentVersions = igsnVersionService.getCurrentVersionForRecord(record, SchemaService.IGSNREGv1);
		igsnVersionService.end(currentVersions, creatorID);
		// Add latest version
		version.setRecord(record);
		version.setCreatedAt(new Date());
		version.setCurrent(true);
		version.setCreatorID(creatorID);
		version.setHash(VersionService.getHash(new String(version.getContent())));
		return igsnVersionService.save(version);
	}

	/**
	 * Helper method to actually mint the IGSN. Creates a new instance of
	 * {@link MDSClient} with the provided
	 * {@link au.edu.ardc.registry.common.model.Allocation} and mint IGSN via the
	 * {@link MDSClient#mintIGSN method}
	 * @param body the Registration Metadata
	 * @param identifierValue the identifierValue in the form of
	 * {prefix}/{namespace}{value}
	 * @param landingPage the URL landing page
	 * @throws Exception bubbled up Exception from
	 * {@link MDSClient#mintIGSN(String, String, String, boolean)}
	 */
	private void mintIGSN(byte[] body, String identifierValue, String landingPage) throws Exception {
		MDSClient mdsClient = new MDSClient(allocation);
		mdsClient.mintIGSN(new String(body), identifierValue, landingPage, false);
	}

}
