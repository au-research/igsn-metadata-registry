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
import au.edu.ardc.registry.exception.NotFoundException;
import au.edu.ardc.registry.exception.RecordNotFoundException;
import au.edu.ardc.registry.exception.TransformerNotFoundException;
import au.edu.ardc.registry.igsn.client.MDSClient;
import au.edu.ardc.registry.igsn.model.IGSNAllocation;
import au.edu.ardc.registry.igsn.transform.ardcv1.ARDCv1ToRegistrationMetadataTransformer;
import org.apache.logging.log4j.core.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
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

	public void registerIdentifier(String identifierValue, Request request) throws Exception {
		logger.info("Registering Identifier " + identifierValue);
		Logger requestLog = igsnRequestService.getLoggerFor(request);

		String supportedSchema = SchemaService.ARDCv1;
		Schema fromSchema = schemaService.getSchemaByID(supportedSchema);

		String allocationID = request.getAttribute(Attribute.ALLOCATION_ID);

		IGSNAllocation allocation = (IGSNAllocation) keycloakService.getAllocationByResourceID(allocationID);

		Identifier identifier = identifierService.findByValueAndType(identifierValue, Identifier.Type.IGSN);

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
		String utcDateTimeStr = Instant.now().toString();
		transformer.setParam("eventType", "registered").setParam("timeStamp", utcDateTimeStr).setParam("registrantName",
				allocation.getMds_username());
		transformer.getParams().forEach((key, value) -> requestLog.debug("Transformer.{}: {}", key, value));
		Version registrationMetadataVersion = transformer.transform(supportedVersion);
		logger.info("Updating Version" + landingPage);
		boolean hasRegistrationMetadataChanged = addVersion(registrationMetadataVersion, record, request);
		// update the registration Metadata at MDS
		if (hasRegistrationMetadataChanged) {
			MDSClient mdsClient = new MDSClient(allocation);
			mdsClient.addMetadata(new String(registrationMetadataVersion.getContent()));
			requestLog.debug("Created and Updated registrationMetadataVersion successfully");
			logger.info("Created and Updated registrationMetadataVersion successfully " + identifierValue);
		}

	}

	/**
	 * Helper method to persist a {@link Version}
	 * @param version the {@link Version} to persist
	 * @param record the {@link Record} to link with
	 * @return boolean true if new version was added
	 */
	private boolean addVersion(Version version, Record record, Request request) {
		Logger requestLog = igsnRequestService.getLoggerFor(request);
		UUID creatorID = UUID.fromString(request.getAttribute(Attribute.CREATOR_ID));
		Version currentVersion = igsnVersionService.getCurrentVersionForRecord(record, SchemaService.IGSNREGv1);
		boolean different = true;
		logger.info("Updating Version" + version.getContent().toString());
		if (currentVersion != null) {
			if (currentVersion.getRequestID() != null && currentVersion.getRequestID().equals(request.getId())) {
				return false;
			}
			byte[] currentContent = currentVersion.getContent();
			byte[] newContent = version.getContent();
			different = XMLUtil.compareRegistrationMetadata(currentVersion.getContent(), version.getContent());
			if (different) {
				igsnVersionService.end(currentVersion, creatorID);
			}
		}
		if (different) {
			version.setRecord(record);
			version.setCreatedAt(request.getCreatedAt());
			version.setCurrent(true);
			version.setHash(VersionService.getHash(new String(version.getContent())));
			version.setCreatorID(creatorID);
			version.setRequestID(request.getId());
			igsnVersionService.save(version);
		}
		return different;
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

}
