package au.edu.ardc.registry.igsn.service;

import au.edu.ardc.registry.common.entity.*;
import au.edu.ardc.registry.common.model.Allocation;
import au.edu.ardc.registry.common.model.Attribute;
import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.provider.LandingPageProvider;
import au.edu.ardc.registry.common.provider.Metadata;
import au.edu.ardc.registry.common.provider.MetadataProviderFactory;
import au.edu.ardc.registry.common.service.*;
import au.edu.ardc.registry.common.transform.TransformerFactory;
import au.edu.ardc.registry.common.util.XMLUtil;
import au.edu.ardc.registry.exception.ForbiddenOperationException;
import au.edu.ardc.registry.exception.NotFoundException;
import au.edu.ardc.registry.exception.RecordNotFoundException;
import au.edu.ardc.registry.exception.TransformerNotFoundException;
import au.edu.ardc.registry.igsn.client.MDSClient;
import au.edu.ardc.registry.igsn.entity.IGSNEventType;
import au.edu.ardc.registry.igsn.model.IGSNAllocation;
import au.edu.ardc.registry.igsn.transform.ardcv1.ARDCv1ToRegistrationMetadataTransformer;
import org.apache.logging.log4j.core.Logger;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;

import java.util.TimeZone;
import java.util.UUID;

@Service
public class IGSNRegistrationService {

	@Autowired
	private IdentifierService identifierService;

	@Autowired
	private IGSNVersionService igsnVersionService;

	@Autowired
	private URLService urlService;

	@Autowired
	private SchemaService schemaService;

	@Autowired
	private IGSNRequestService igsnRequestService;

	@Autowired
	private KeycloakService keycloakService;

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(IGSNRegistrationService.class);

	/**
	 * @param identifierValue {@link Identifier}'s Value as String
	 * @param request the {@link Request} this identifier is created/updated by
	 * @throws Exception if registering or updating fails throws
	 */
	public void registerIdentifier(String identifierValue, Request request) throws Exception {
		logger.info("Registering Identifier " + identifierValue);
		Logger requestLog = igsnRequestService.getLoggerFor(request);

		String supportedSchema = SchemaService.ARDCv1;
		Schema fromSchema = schemaService.getSchemaByID(supportedSchema);

		String allocationID = request.getAttribute(Attribute.ALLOCATION_ID);

		IGSNAllocation allocation = (IGSNAllocation) keycloakService.getAllocationByResourceID(allocationID);

		// obtain the Identifier
		Identifier identifier = identifierService.findByValueAndType(identifierValue, Identifier.Type.IGSN);

		if (identifier == null) {
			requestLog.error("Failed to obtain Identifier : {} with Type: {}", identifierValue, Identifier.Type.IGSN);
			throw new ForbiddenOperationException(String.format("Identifier with value %s and type %s doesn't exist",
					identifierValue, Identifier.Type.IGSN));
		}

		// obtain record
		Record record = identifier.getRecord();
		if (record == null) {
			requestLog.error("Failed to obtain associated record for identifier: {}", identifier.getId());
			throw new RecordNotFoundException(identifier.getId().toString());
		}

		// obtain version

		Version supportedVersion = igsnVersionService.getCurrentVersionForRecord(record, supportedSchema);
		if (supportedVersion == null) {
			requestLog.error("Failed to generate registration metadata with schema {}", supportedSchema);
			throw new NotFoundException(
					"Unable to generate registration metadata missing " + supportedSchema + " version");
		}

		LandingPageProvider landingPageProvider = (LandingPageProvider) MetadataProviderFactory.create(fromSchema,
				Metadata.LandingPage);

		String landingPage = landingPageProvider.get(new String(supportedVersion.getContent()));

		boolean hasLandingPageChanged = updateLandingPage(landingPage, record, request);
		// Update the URL of the IGSN at MDS

		if (hasLandingPageChanged) {
			MDSClient mdsClient = new MDSClient(allocation);
			mdsClient.createOrUpdateIdentifier(identifierValue, landingPage);
			requestLog.debug("Landing Page updated {}", landingPage);
			logger.info("Landing Page updated " + landingPage);
		}

		// transform to registration metadata

		Schema toSchema = schemaService.getSchemaByID(SchemaService.IGSNREGv1);

		// obtain landing page
		logger.debug("fromSchema: {}", fromSchema);
		logger.debug("toSchema: {}", toSchema);

		ARDCv1ToRegistrationMetadataTransformer transformer = (ARDCv1ToRegistrationMetadataTransformer) TransformerFactory
				.create(fromSchema, toSchema);

		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		df.setTimeZone(tz);
		String utcDateTimeStr = df.format(request.getCreatedAt());
		String eventType = getEventType(request.getType());
		transformer.setParam("eventType", eventType).setParam("timeStamp", utcDateTimeStr).setParam("registrantName",
				allocation.getMds_username());
		transformer.getParams().forEach((key, value) -> requestLog.debug("Transformer.{}: {}", key, value));
		Version registrationMetadataVersion = transformer.transform(supportedVersion);
		logger.info("Updating Version" + landingPage);
		boolean hasRegistrationMetadataChanged = addRegistrationMetadataVersion(registrationMetadataVersion, record,
				request);
		// update the registration Metadata at MDS
		if (hasRegistrationMetadataChanged) {
			MDSClient mdsClient = new MDSClient(allocation);
			mdsClient.addMetadata(new String(registrationMetadataVersion.getContent()));
			requestLog.debug("Created and Updated registrationMetadataVersion successfully");
			logger.info("Created and Updated registrationMetadataVersion successfully " + identifierValue);
		}

	}

	/**
	 * Helper method to add a registration Metadata version and set it to current
	 * @param version the {@link Version} to persist
	 * @param record the {@link Record} to link with
	 * @param request the {@link Request} that created the update
	 * @return boolean true if new version was added
	 */
	private boolean addRegistrationMetadataVersion(@NotNull Version version, Record record, Request request) {
		Logger requestLog = igsnRequestService.getLoggerFor(request);
		UUID creatorID = UUID.fromString(request.getAttribute(Attribute.CREATOR_ID));
		Version currentVersion = igsnVersionService.getCurrentVersionForRecord(record, SchemaService.IGSNREGv1);

		logger.info("Updating Version" + version.getContent().toString());
		if (currentVersion != null) {
			// the version is later than the current request (shouldn't happen unless requests are re-run
			if(currentVersion.getCreatedAt().after(request.getCreatedAt())){
				logger.info("Current Version is newer" + version.getContent().toString());
				return false;
			}
			// the current version was created by the same request
			if (currentVersion.getRequestID() != null && currentVersion.getRequestID().equals(request.getId())) {
				logger.info("Current Version already the same" + version.getContent().toString());
				return false;
			}
			// there is current version then end it now
			igsnVersionService.end(currentVersion, creatorID);
		}
		// if we got this far a new version will be added
		version.setRecord(record);
		version.setCreatedAt(request.getCreatedAt());
		version.setCurrent(true);
		version.setCreatorID(creatorID);
		version.setRequestID(request.getId());
		igsnVersionService.save(version);

		return true;
	}

	/**
	 * @param landingPage the url of the landing page
	 * @param record the record of the landing page
	 * @param request the request this version was provided by
	 * @return boolean (true) if the landing page is new or different
	 */
	private boolean updateLandingPage(String landingPage, Record record, Request request) {
		URL url = urlService.findByRecord(record);

		// if it's the first time this record is created
		if (url == null) {
			url = new URL();
			url.setRecord(record);
			url.setUrl(landingPage);
			url.setResolvable(true);
			url.setCreatedAt(request.getCreatedAt());
			urlService.create(url);
			return true;
		}
		// if the landingpage didn't change
		else if (url.getUrl().toLowerCase().equals(landingPage.toLowerCase())) {
			return false;
		}
		// set the url to the current landing page
		else {
			url.setUrl(landingPage);
			urlService.update(url);
			return true;
		}
	}

	/**
	 * a very crude method to map between igsnservice events to registration metadata event
	 * @param eventType an IGSN Service event Type
	 * @return String supported reg 1.0 1.1 event types
	 * https://github.com/IGSN/metadata/blob/master/registration/1.0/include/igsn-eventType-v1.0.xsd
	 * <xs:enumeration value="submitted"/><!-- Date of the initial registration. -->
	 * <xs:enumeration value="registered"/><!-- The object is registered. -->
	 * <xs:enumeration value="updated"/><!-- Date of the last metadata update. -->
	 * <xs:enumeration value="deprecated"/>
	 * <!--The object description is deprecated. The entry is no longer relevant, e.g. due to duplicate registration.-->
	 * <xs:enumeration value="destroyed"/><!-- The object is destroyed. -->
	 *
	 */
	private String getEventType(String eventType){
		if(eventType.toLowerCase().contains("update")){
			return "updated";
		}
		if(eventType.toLowerCase().contains("mint")){
			return "registered";
		}
		return "updated";
	}

}
