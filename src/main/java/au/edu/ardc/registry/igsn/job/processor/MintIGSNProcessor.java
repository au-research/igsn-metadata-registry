package au.edu.ardc.registry.igsn.job.processor;

import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.provider.LandingPageProvider;
import au.edu.ardc.registry.common.provider.Metadata;
import au.edu.ardc.registry.common.provider.MetadataProviderFactory;
import au.edu.ardc.registry.common.repository.IdentifierRepository;
import au.edu.ardc.registry.common.repository.VersionRepository;
import au.edu.ardc.registry.common.service.*;
import au.edu.ardc.registry.common.transform.TransformerFactory;
import au.edu.ardc.registry.exception.ContentProviderNotFoundException;
import au.edu.ardc.registry.exception.NotFoundException;
import au.edu.ardc.registry.exception.TransformerNotFoundException;
import au.edu.ardc.registry.igsn.client.MDSClient;
import au.edu.ardc.registry.igsn.model.IGSNAllocation;
import au.edu.ardc.registry.igsn.service.IGSNVersionService;
import au.edu.ardc.registry.igsn.transform.ardcv1.ARDCv1ToRegistrationMetadataTransformer;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;

import java.time.Instant;
import java.util.Date;

public class MintIGSNProcessor implements ItemProcessor<String, String> {

	private SchemaService schemaService;

	private KeycloakService kcService;

	private IdentifierService identifierService;

	private RecordService recordService;

	private IGSNVersionService igsnVersionService;

	private URLService urlService;

	private String supportedSchema = SchemaService.ARDCv1;

	private Version existingRegistrationMDVersion;

	private IGSNAllocation allocation;

	private String landingPage;

	public MintIGSNProcessor(SchemaService schemaService, KeycloakService kcService,
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
	public String process(@NotNull String identifierValue) throws Exception {
		String result = "";
		Identifier identifier = identifierService.findByValueAndType(identifierValue, Identifier.Type.IGSN);
		byte[] registrationMetaBody = addRegistrationMetadata(identifier);
		mintIGSN(registrationMetaBody, identifierValue, landingPage);
		identifier.setStatus(Identifier.Status.ACCESSIBLE);
		identifierService.update(identifier);
		return result;

	}

	@BeforeStep
	public void beforeStep(final StepExecution stepExecution) throws Exception {
		JobParameters jobParameters = stepExecution.getJobParameters();
		String IGSNServiceRequestID = jobParameters.getString("IGSNServiceRequestID");
		String allocationID = jobParameters.getString("allocationID");
		allocation = (IGSNAllocation) kcService.getAllocationByResourceID(allocationID);

	}

	private byte[] addRegistrationMetadata(Identifier identifier)
			throws TransformerNotFoundException, NotFoundException, ContentProviderNotFoundException {

		Record record = identifier.getRecord();
		Version supportedVersion = igsnVersionService.getCurrentVersionForRecord(record, supportedSchema);
		ARDCv1ToRegistrationMetadataTransformer transformer = null;

		if (supportedVersion == null) {
			throw new NotFoundException(
					"Unable to generate registration metadata missing " + supportedSchema + " version");
		}

		try {

			Schema fromSchema = schemaService.getSchemaByID(supportedSchema);
			Schema toSchema = schemaService.getSchemaByID(SchemaService.IGSNREGv1);
			transformer = (ARDCv1ToRegistrationMetadataTransformer) TransformerFactory.create(fromSchema, toSchema);
			LandingPageProvider landingPageProvider = (LandingPageProvider) MetadataProviderFactory.create(fromSchema,
					Metadata.LandingPage);
			landingPage = landingPageProvider.get(new String(supportedVersion.getContent()));

		}
		catch (TransformerNotFoundException e) {
			throw e;
		}

		// TODO get some user info
		/**
		 * <xs:enumeration value="submitted"/> <!-- Date of the initial registration. -->
		 * <xs:enumeration value="registered"/> <!-- The object is registered. -->
		 * <xs:enumeration value="updated"/> <!-- Date of the last metadata update. -->
		 * <xs:enumeration value="deprecated"/> <!-- The object description is deprecated.
		 * The entry is no longer relevant, e.g. due to duplicate registration. -->
		 * <xs:enumeration value="destroyed"/>
		 *
		 *
		 * .setParam("registrantName") .setParam("nameIdentifier")
		 * .setParam("nameIdentifierScheme")
		 *
		 */
		String utcDateTimeStr = Instant.now().toString();
		transformer.setParam("eventType", "registered").setParam("timeStamp", utcDateTimeStr).setParam("registrantName",
				allocation.getMds_username());

		Version registrationMetadataVersion = transformer.transform(supportedVersion);
		addVersion(registrationMetadataVersion, record);
		return registrationMetadataVersion.getContent();
	}

	private void addVersion(Version version, Record record) {
		version.setRecord(record);
		version.setCreatedAt(new Date());
		version.setCurrent(true);
		version.setHash(VersionService.getHash(new String(version.getContent())));
		System.out.println("addVersion Registration meta Version:" + version.getId());
		igsnVersionService.save(version);
	}

	private int mintIGSN(byte[] body, String identifierValue, String landingPage) throws Exception {
		boolean success = false;
		MDSClient mdsClient = new MDSClient(allocation);
		return mdsClient.mintIGSN(new String(body), identifierValue, landingPage, false);
	}

}
