package au.edu.ardc.registry.igsn.controller;

import au.edu.ardc.registry.common.dto.AllocationDTO;
import au.edu.ardc.registry.common.dto.RequestDTO;
import au.edu.ardc.registry.common.dto.mapper.RequestMapper;
import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.model.Attribute;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.provider.FragmentProvider;
import au.edu.ardc.registry.common.provider.IdentifierProvider;
import au.edu.ardc.registry.common.provider.Metadata;
import au.edu.ardc.registry.common.provider.MetadataProviderFactory;
import au.edu.ardc.registry.common.service.IdentifierService;
import au.edu.ardc.registry.common.service.KeycloakService;
import au.edu.ardc.registry.common.service.RequestService;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.exception.APIExceptionResponse;
import au.edu.ardc.registry.igsn.model.IGSNAllocation;
import au.edu.ardc.registry.igsn.service.IGSNRequestService;
import au.edu.ardc.registry.igsn.service.IGSNRequestValidationService;
import au.edu.ardc.registry.igsn.service.IGSNService;
import au.edu.ardc.registry.igsn.service.ImportService;
import au.edu.ardc.registry.igsn.task.ImportIGSNTask;
import au.edu.ardc.registry.igsn.task.TransferIGSNTask;
import au.edu.ardc.registry.igsn.task.UpdateIGSNTask;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/services/igsn",
		produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
@ConditionalOnProperty(name = "app.igsn.enabled")
@Tag(name = "IGSN Service", description = "API endpoints for IGSN related operations")
@SecurityRequirement(name = "basic")
@SecurityRequirement(name = "oauth2")
public class IGSNServiceController {

	private static final Logger logger = LoggerFactory.getLogger(IGSNServiceController.class);

	final RequestService requestService;

	final RequestMapper requestMapper;

	final IGSNService igsnService;

	private final IGSNRequestService igsnRequestService;

	private final KeycloakService keycloakService;

	private final IGSNRequestValidationService igsnRequestValidationService;

	private final ApplicationEventPublisher applicationEventPublisher;

	private final ImportService importService;

	private final SchemaService schemaService;

	private final IdentifierService identifierService;

	@Value("${request.max-single-filesize:6144}")
	long maxSingleFileSize;

	@Value("${request.max-bulk-filesize:5242880}")
	long maxBulkFileSize;

	@Value("${request.max-records-per-request:1000}")
	int maxNumOfRecords = 1000;

	public IGSNServiceController(IGSNRequestService igsnRequestService, RequestService requestService,
								 RequestMapper requestMapper, KeycloakService keycloakService,
								 IGSNRequestValidationService igsnRequestValidationService, IGSNService igsnService,
								 ApplicationEventPublisher applicationEventPublisher, ImportService importService,
								 SchemaService schemaService, IdentifierService identifierService) {
		this.igsnRequestService = igsnRequestService;
		this.requestService = requestService;
		this.requestMapper = requestMapper;
		this.keycloakService = keycloakService;
		this.igsnRequestValidationService = igsnRequestValidationService;
		this.igsnService = igsnService;
		this.applicationEventPublisher = applicationEventPublisher;
		this.importService = importService;
		this.schemaService = schemaService;
		this.identifierService = identifierService;
	}

	@PostMapping(value = "/bulk-mint", consumes = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE,
			MediaType.TEXT_PLAIN_VALUE })
	@Operation(summary = "Bulk mint IGSN", description = "Creates several IGSNs in a single payload",
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "The descriptive metadata of up to 1000 IGSN records or 5MB",
					content= @Content(examples = {
							@ExampleObject(name="ARDC v1 XML", value="<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
									"<resources xmlns=\"https://identifiers.ardc.edu.au/schemas/ardc-igsn-desc\">\n" +
									"    <resource registeredObjectType=\"http://pid.geoscience.gov.au/def/voc/ga/igsncode/PhysicalSample\">\n" +
									"        <resourceIdentifier>20.500.11812/XXZT10000001</resourceIdentifier>\n" +
									"        <landingPage>https://test.identifiers.ardc.edu.au/igsn-portal/view/20.500.11812/XXZT10000001</landingPage>\n" +
									"        <isPublic>true</isPublic>\n" +
									"        <resourceTitle>A Rock sample (Sample JBS35 / IGSN XXZT10000001)</resourceTitle>\n" +
									"        <resourceTypes>\n" +
									"            <resourceType>http://vocabulary.odm2.org/specimentype/other</resourceType>\n" +
									"        </resourceTypes>\n" +
									"        <materialTypes>\n" +
									"            <materialType>http://vocabulary.odm2.org/medium/mineral</materialType>\n" +
									"        </materialTypes>\n" +
									"        <classifications>\n" +
									"            <classification>igneous granitic (granite)</classification>\n" +
									"        </classifications>\n" +
									"        <sampledFeatures>\n" +
									"            <sampledFeature>Sholl Belt</sampledFeature>\n" +
									"        </sampledFeatures>\n" +
									"        <location>\n" +
									"            <locality>2km inside the south margin of the granitoid</locality>\n" +
									"            <geometry srid=\"https://epsg.io/4326\">POINT (116.851 -20.829)</geometry>\n" +
									"        </location>\n" +
									"        <curationDetails>\n" +
									"            <curation>\n" +
									"                <curator>\n" +
									"                    <curatorName>Geological Survey of Western Australia</curatorName>\n" +
									"                </curator>\n" +
									"                <curationDate>2019-06-21</curationDate>\n" +
									"                <curationLocation>Bentley</curationLocation>\n" +
									"                <curatingInstitution institutionURI=\"http://www.curtin.edu.au\">Curtin University</curatingInstitution>\n" +
									"            </curation>\n" +
									"        </curationDetails>\n" +
									"        <contributors>\n" +
									"            <contributor contributorType=\"http://registry.it.csiro.au/def/isotc211/CI_RoleCode/pointOfContact\">\n" +
									"                <contributorName>Dwayne Douglas Johnson</contributorName>\n" +
									"            </contributor>\n" +
									"        </contributors>\n" +
									"        <comments>Data has been provided by the Dwayne Johnson Collection.</comments>\n" +
									"        <logDate eventType=\"registered\">1995</logDate>\n" +
									"    </resource>\n" +
									"        <resource registeredObjectType=\"http://pid.geoscience.gov.au/def/voc/ga/igsncode/PhysicalSample\">\n" +
									"        <resourceIdentifier>20.500.11812/XXZT10000002</resourceIdentifier>\n" +
									"        <landingPage>https://test.identifiers.ardc.edu.au/igsn-portal/view/20.500.11812/XXZT10000002</landingPage>\n" +
									"        <isPublic>true</isPublic>\n" +
									"        <resourceTitle>A second Rock sample (Sample JBS34 / IGSN XXZT10000002)</resourceTitle>\n" +
									"        <resourceTypes>\n" +
									"            <resourceType>http://vocabulary.odm2.org/specimentype/other</resourceType>\n" +
									"        </resourceTypes>\n" +
									"        <materialTypes>\n" +
									"            <materialType>http://vocabulary.odm2.org/medium/mineral</materialType>\n" +
									"        </materialTypes>\n" +
									"        <classifications>\n" +
									"            <classification>igneous granitic (granite)</classification>\n" +
									"        </classifications>\n" +
									"        <sampledFeatures>\n" +
									"            <sampledFeature>Sholl Belt</sampledFeature>\n" +
									"        </sampledFeatures>\n" +
									"        <location>\n" +
									"            <locality>2km inside the south margin of the granitoid</locality>\n" +
									"            <geometry srid=\"https://epsg.io/4326\">POINT (116.851 -20.829)</geometry>\n" +
									"        </location>\n" +
									"        <curationDetails>\n" +
									"            <curation>\n" +
									"                <curator>\n" +
									"                    <curatorName>Geological Survey of Western Australia</curatorName>\n" +
									"                </curator>\n" +
									"                <curationDate>2019-06-21</curationDate>\n" +
									"                <curationLocation>Bentley</curationLocation>\n" +
									"                <curatingInstitution institutionURI=\"http://www.curtin.edu.au\">Curtin University</curatingInstitution>\n" +
									"            </curation>\n" +
									"        </curationDetails>\n" +
									"        <contributors>\n" +
									"            <contributor contributorType=\"http://registry.it.csiro.au/def/isotc211/CI_RoleCode/pointOfContact\">\n" +
									"                <contributorName>Dwayne Douglas Johnson</contributorName>\n" +
									"            </contributor>\n" +
									"        </contributors>\n" +
									"        <comments>Data has been provided by the Dwayne Johnson Collection.</comments>\n" +
									"        <logDate eventType=\"registered\">1995</logDate>\n" +
									"    </resource>\n" +
									"</resources>"),
							@ExampleObject(name="CSIRO v3 XML", value="<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
									"<resources xmlns=\"https://igsn.csiro.au/schemas/3.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"https://igsn.csiro.au/schemas/3.0 https://igsn.csiro.au/schemas/3.0/igsn-csiro-v3.0.xsd\">\n" +
									"    <resource registeredObjectType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/PhysicalSample\">\n" +
									"        <resourceIdentifier>XXZT10000001</resourceIdentifier>\n" +
									"        <landingPage>https://test.identifiers.ardc.edu.au/igsn-portal/view/20.500.11812/XXZT10000001</landingPage>\n" +
									"        <isPublic>true</isPublic>\n" +
									"        <resourceTitle>A rock sample from the Dwayne Johnsony Collection</resourceTitle>\n" +
									"        <alternateIdentifiers>\n" +
									"            <alternateIdentifier>An alternative Identifier</alternateIdentifier>\n" +
									"            <alternateIdentifier>A second alternative Identifer</alternateIdentifier>\n" +
									"        </alternateIdentifiers>\n" +
									"        <resourceTypes>\n" +
									"            <resourceType>http://vocabulary.odm2.org/specimentype/core</resourceType>\n" +
									"            <resourceType>http://vocabulary.odm2.org/specimentype/corePiece</resourceType>\n" +
									"        </resourceTypes>\n" +
									"        <materialTypes>\n" +
									"            <materialType>http://vocabulary.odm2.org/medium/particulate</materialType>\n" +
									"            <materialType>http://vocabulary.odm2.org/medium/air</materialType>\n" +
									"        </materialTypes>\n" +
									"        <classifications>\n" +
									"            <classification classificationURI=\"http://www.classification.com/tin\">Some phrases for classification</classification>\n" +
									"            <classification classificationURI=\"http://www.classification.com/gold\">maybe gold or silver</classification>\n" +
									"        </classifications>\n" +
									"        <purpose>This is a test resource for demo single</purpose>\n" +
									"        <sampledFeatures>\n" +
									"            <sampledFeature sampledFeatureURI=\"http://www.samplefeature.com/uri/paper\">Paper covers rock</sampledFeature>\n" +
									"            <sampledFeature sampledFeatureURI=\"http://www.samplefeature.com/uri/scisors\">Scissors cuts paper</sampledFeature>\n" +
									"            <sampledFeature sampledFeatureURI=\"http://www.samplefeature.com/uri/rock\">Rock breaks scissors</sampledFeature>\n" +
									"        </sampledFeatures>\n" +
									"        <location>\n" +
									"            <locality localityURI=\"http://google.map/perth\">Canning vale</locality>\n" +
									"            <geometry srid=\"https://epsg.io/4326\" verticalDatum=\"https://epsg.io/4326\" geometryURI=\"http://www.altova.com\">POLYGON ((127.05688476563 -20.5224609375, 124.24438476563 -28.2568359375, 143.22875976563 -32.1240234375, 142.17407226563 -20.8740234375, 127.05688476563 -20.5224609375))</geometry>\n" +
									"        </location>\n" +
									"        <date>\n" +
									"            <timePeriod>\n" +
									"                <start>2003</start>\n" +
									"                <end>2002</end>\n" +
									"            </timePeriod>\n" +
									"        </date>\n" +
									"        <method methodURI=\"http://method.com/collection\">Lab sampling</method>\n" +
									"        <campaign>a</campaign>\n" +
									"        <curationDetails>\n" +
									"            <curation>\n" +
									"                <curator>Curtin</curator>\n" +
									"                <curationDate>2001-12</curationDate>\n" +
									"                <curationLocation>Bentley</curationLocation>\n" +
									"                <curatingInstitution institutionURI=\"http://www.curtin.org\">Curtin University</curatingInstitution>\n" +
									"            </curation>\n" +
									"            <curation>\n" +
									"                <curator>CSIRO</curator>\n" +
									"                <curationDate>2001-12</curationDate>\n" +
									"                <curationLocation>In the lab somewhere</curationLocation>\n" +
									"                <curatingInstitution institutionURI=\"http://csiro.au\">ARRC</curatingInstitution>\n" +
									"            </curation>\n" +
									"        </curationDetails>\n" +
									"        <contributors>\n" +
									"            <contributor contributorType=\"http://registry.it.csiro.au/def/isotc211/CI_RoleCode/pointOfContact\">\n" +
									"                <contributorName>Dwayne Johnsony</contributorName>\n" +
									"                <contributorIdentifier contributorIdentifierType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/ORCID\">WWE0002</contributorIdentifier>\n" +
									"            </contributor>\n" +
									"            <contributor contributorType=\"http://registry.it.csiro.au/def/isotc211/CI_RoleCode/originator\">\n" +
									"                <contributorName>Dwayne Douglas Johnson</contributorName>\n" +
									"                <contributorIdentifier contributorIdentifierType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/EISSN\">WWF0001</contributorIdentifier>\n" +
									"            </contributor>\n" +
									"        </contributors>\n" +
									"        <relatedResources>\n" +
									"            <relatedResource relatedResourceIdentifierType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/ARK\" relationType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/HasDigitalRepresentation\">A related resource somewhere</relatedResource>\n" +
									"            <relatedResource relatedResourceIdentifierType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/DOI\" relationType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/IsMemberOf\">A related resource somewhere</relatedResource>\n" +
									"        </relatedResources>\n" +
									"        <comments>This is a comment section about the rock samples</comments>\n" +
									"        <logDate eventType=\"registered\">2002</logDate>\n" +
									"    </resource>\n" +
									"    <resource registeredObjectType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/PhysicalSample\">\n" +
									"        <resourceIdentifier>XXZT10000002</resourceIdentifier>\n" +
									"        <landingPage>https://test.identifiers.ardc.edu.au/igsn-portal/view/20.500.11812/XXZT10000002</landingPage>\n" +
									"        <isPublic>true</isPublic>\n" +
									"        <resourceTitle>A Second rock sample from the Dwayne Johnsony Collection</resourceTitle>\n" +
									"        <alternateIdentifiers>\n" +
									"            <alternateIdentifier>An alternative Identifier</alternateIdentifier>\n" +
									"            <alternateIdentifier>A second alternative Identifer</alternateIdentifier>\n" +
									"        </alternateIdentifiers>\n" +
									"        <resourceTypes>\n" +
									"            <resourceType>http://vocabulary.odm2.org/specimentype/core</resourceType>\n" +
									"            <resourceType>http://vocabulary.odm2.org/specimentype/corePiece</resourceType>\n" +
									"        </resourceTypes>\n" +
									"        <materialTypes>\n" +
									"            <materialType>http://vocabulary.odm2.org/medium/particulate</materialType>\n" +
									"            <materialType>http://vocabulary.odm2.org/medium/air</materialType>\n" +
									"        </materialTypes>\n" +
									"        <classifications>\n" +
									"            <classification classificationURI=\"http://www.classification.com/tin\">Some phrases for classification</classification>\n" +
									"            <classification classificationURI=\"http://www.classification.com/gold\">maybe gold or silver</classification>\n" +
									"        </classifications>\n" +
									"        <purpose>This is a test resource for demo single</purpose>\n" +
									"        <sampledFeatures>\n" +
									"            <sampledFeature sampledFeatureURI=\"http://www.samplefeature.com/uri/paper\">Paper covers rock</sampledFeature>\n" +
									"            <sampledFeature sampledFeatureURI=\"http://www.samplefeature.com/uri/scisors\">Scissors cuts paper</sampledFeature>\n" +
									"            <sampledFeature sampledFeatureURI=\"http://www.samplefeature.com/uri/rock\">Rock breaks scissors</sampledFeature>\n" +
									"        </sampledFeatures>\n" +
									"        <location>\n" +
									"            <locality localityURI=\"http://google.map/perth\">Canning vale</locality>\n" +
									"            <geometry srid=\"https://epsg.io/4326\" verticalDatum=\"https://epsg.io/4326\" geometryURI=\"http://www.altova.com\">POLYGON ((127.05688476563 -20.5224609375, 124.24438476563 -28.2568359375, 143.22875976563 -32.1240234375, 142.17407226563 -20.8740234375, 127.05688476563 -20.5224609375))</geometry>\n" +
									"        </location>\n" +
									"        <date>\n" +
									"            <timePeriod>\n" +
									"                <start>2003</start>\n" +
									"                <end>2002</end>\n" +
									"            </timePeriod>\n" +
									"        </date>\n" +
									"        <method methodURI=\"http://method.com/collection\">Lab sampling</method>\n" +
									"        <campaign>a</campaign>\n" +
									"        <curationDetails>\n" +
									"            <curation>\n" +
									"                <curator>Curtin</curator>\n" +
									"                <curationDate>2001-12</curationDate>\n" +
									"                <curationLocation>Bentley</curationLocation>\n" +
									"                <curatingInstitution institutionURI=\"http://www.curtin.org\">Curtin University</curatingInstitution>\n" +
									"            </curation>\n" +
									"            <curation>\n" +
									"                <curator>CSIRO</curator>\n" +
									"                <curationDate>2001-12</curationDate>\n" +
									"                <curationLocation>In the lab somewhere</curationLocation>\n" +
									"                <curatingInstitution institutionURI=\"http://csiro.au\">ARRC</curatingInstitution>\n" +
									"            </curation>\n" +
									"        </curationDetails>\n" +
									"        <contributors>\n" +
									"            <contributor contributorType=\"http://registry.it.csiro.au/def/isotc211/CI_RoleCode/pointOfContact\">\n" +
									"                <contributorName>Dwayne Johnsony</contributorName>\n" +
									"                <contributorIdentifier contributorIdentifierType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/ORCID\">WWE0002</contributorIdentifier>\n" +
									"            </contributor>\n" +
									"            <contributor contributorType=\"http://registry.it.csiro.au/def/isotc211/CI_RoleCode/originator\">\n" +
									"                <contributorName>Dwayne Douglas Johnson</contributorName>\n" +
									"                <contributorIdentifier contributorIdentifierType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/EISSN\">WWF0001</contributorIdentifier>\n" +
									"            </contributor>\n" +
									"        </contributors>\n" +
									"        <relatedResources>\n" +
									"            <relatedResource relatedResourceIdentifierType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/ARK\" relationType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/HasDigitalRepresentation\">A related resource somewhere</relatedResource>\n" +
									"            <relatedResource relatedResourceIdentifierType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/DOI\" relationType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/IsMemberOf\">A related resource somewhere</relatedResource>\n" +
									"        </relatedResources>\n" +
									"        <comments>This is a comment section about the rock samples</comments>\n" +
									"        <logDate eventType=\"registered\">2002</logDate>\n" +
									"    </resource>\n" +
									"</resources>")})),
			parameters = { @Parameter(name = "ownerID",
					description = "The UUID of the intended Owner, if the OwnerType value is set to User, this value must be equal to the User's UUID.",
					schema = @Schema(implementation = UUID.class)),
					@Parameter(name = "ownerType", description = "The Type of the Owner",
							schema = @Schema(description = "Owner Type", type = "string",
									allowableValues = { "User", "DataCenter" })) },
			responses = {
					@ApiResponse(responseCode = "200", description = "Bulk Mint request is accepted",
							content = @Content(schema = @Schema(implementation = RequestDTO.class))),
					@ApiResponse(responseCode = "403", description = "Forbidden Operation Exception",
							content = @Content(schema = @Schema(implementation = APIExceptionResponse.class))),
					@ApiResponse(responseCode = "400", description = "Validation Exception",
							content = @Content(schema = @Schema(implementation = APIExceptionResponse.class))) })
	public ResponseEntity<RequestDTO> bulkMint(HttpServletRequest httpServletRequest, @RequestBody String payload,
											   @RequestParam(required = false) String ownerID,
											   @RequestParam(required = false, defaultValue = "User") String ownerType) throws IOException {
		User user = keycloakService.getLoggedInUser(httpServletRequest);

		// creating the IGSN Request & write the payload to file
		Request request = igsnRequestService.createRequest(user, IGSNService.EVENT_BULK_MINT, payload);
		httpServletRequest.setAttribute(String.valueOf(Request.class), request);

		request.setAttribute(Attribute.OWNER_TYPE, ownerType);
		request.setAttribute(Attribute.CREATOR_ID, user.getId().toString());
		if (ownerID != null) {
			request.setAttribute(Attribute.OWNER_ID, ownerID);
		}
		else {
			request.setAttribute(Attribute.OWNER_ID, user.getId().toString());
		}

		// Validate the request
		igsnRequestValidationService.validate(request, user);
		request.setStatus(Request.Status.ACCEPTED);
		igsnRequestService.save(request);

		// Queue request

		// process the request (async)
		request.setStatus(Request.Status.QUEUED);
		request.setMessage("Bulk Mint Request is Queued");
		igsnRequestService.save(request);
		igsnService.processMintOrUpdate(request);

		RequestDTO dto = requestMapper.getConverter().convert(request);
		return ResponseEntity.ok().body(dto);
	}

	/**
	 * Mint IGSN Service endpoint.
	 * @param httpServletRequest the {@link HttpServletRequest} for this request
	 * @param payload the required {@link RequestBody} for this request background job or
	 * @param ownerID (optional) the UUID of the owner of the newly minted record
	 * @param ownerType (User or Datacenter) the Type of the owner wait until mint is
	 * completed default is {no , false, 0}
	 * @return an IGSN response records
	 * @throws Exception when things go wrong, handled by Exception Advice
	 */
	@PostMapping(value = "/mint", consumes = { MediaType.APPLICATION_XML_VALUE})
	@Operation(summary = "Mint a new IGSN", description = "Creates a new IGSN Identifier and Metadata",
			//requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "the XML payload"),
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
					description = "The Descriptive XML metadata of 1 IGSN record max size 60KB",
//					content= @Content(examples = {
//							@ExampleObject(name="ARDC v1 XML", externalValue="http://localhost:8085/igsn-registry/examples/sample_ardc_v1.xml"),
//							@ExampleObject(name="CSIRO v3 XML", externalValue="http://localhost:8085/igsn-registry/examples/sample_csiro_v3.xml")})),
					content= @Content(examples = {
							@ExampleObject(name="ARDC v1 XML", value="<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
									"<resources xmlns=\"https://identifiers.ardc.edu.au/schemas/ardc-igsn-desc\">\n" +
									"    <resource registeredObjectType=\"http://pid.geoscience.gov.au/def/voc/ga/igsncode/PhysicalSample\">\n" +
									"        <resourceIdentifier>20.500.11812/XXZT10000001</resourceIdentifier>\n" +
									"        <landingPage>https://test.identifiers.ardc.edu.au/igsn-portal/view/20.500.11812/XXZT10000001</landingPage>\n" +
									"        <isPublic>true</isPublic>\n" +
									"        <resourceTitle>A Rock sample (Sample JBS35 / IGSN XXZT10000001)</resourceTitle>\n" +
									"        <resourceTypes>\n" +
									"            <resourceType>http://vocabulary.odm2.org/specimentype/other</resourceType>\n" +
									"        </resourceTypes>\n" +
									"        <materialTypes>\n" +
									"            <materialType>http://vocabulary.odm2.org/medium/mineral</materialType>\n" +
									"        </materialTypes>\n" +
									"        <classifications>\n" +
									"            <classification>igneous granitic (granite)</classification>\n" +
									"        </classifications>\n" +
									"        <sampledFeatures>\n" +
									"            <sampledFeature>Sholl Belt</sampledFeature>\n" +
									"        </sampledFeatures>\n" +
									"        <location>\n" +
									"            <locality>2km inside the south margin of the granitoid</locality>\n" +
									"            <geometry srid=\"https://epsg.io/4326\">POINT (116.851 -20.829)</geometry>\n" +
									"        </location>\n" +
									"        <curationDetails>\n" +
									"            <curation>\n" +
									"                <curator>\n" +
									"                    <curatorName>Geological Survey of Western Australia</curatorName>\n" +
									"                </curator>\n" +
									"                <curationDate>2019-06-21</curationDate>\n" +
									"                <curationLocation>Bentley</curationLocation>\n" +
									"                <curatingInstitution institutionURI=\"http://www.curtin.edu.au\">Curtin University</curatingInstitution>\n" +
									"            </curation>\n" +
									"        </curationDetails>\n" +
									"        <contributors>\n" +
									"            <contributor contributorType=\"http://registry.it.csiro.au/def/isotc211/CI_RoleCode/pointOfContact\">\n" +
									"                <contributorName>Dwayne Douglas Johnson</contributorName>\n" +
									"            </contributor>\n" +
									"        </contributors>\n" +
									"        <comments>Data has been provided by the Dwayne Johnson Collection.</comments>\n" +
									"        <logDate eventType=\"registered\">1995</logDate>\n" +
									"    </resource>\n" +
									"</resources>"),
							@ExampleObject(name="CSIRO v3 XML", value="<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
									"<resources xmlns=\"https://igsn.csiro.au/schemas/3.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"https://igsn.csiro.au/schemas/3.0 https://igsn.csiro.au/schemas/3.0/igsn-csiro-v3.0.xsd\">\n" +
									"    <resource registeredObjectType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/PhysicalSample\">\n" +
									"        <resourceIdentifier>XXZT10000001</resourceIdentifier>\n" +
									"        <landingPage>https://test.identifiers.ardc.edu.au/igsn-portal/view/20.500.11812/XXZT10000001</landingPage>\n" +
									"        <isPublic>true</isPublic>\n" +
									"        <resourceTitle>A rock sample from the Dwayne Johnsony Collection</resourceTitle>\n" +
									"        <alternateIdentifiers>\n" +
									"            <alternateIdentifier>An alternative Identifier</alternateIdentifier>\n" +
									"            <alternateIdentifier>A second alternative Identifer</alternateIdentifier>\n" +
									"        </alternateIdentifiers>\n" +
									"        <resourceTypes>\n" +
									"            <resourceType>http://vocabulary.odm2.org/specimentype/core</resourceType>\n" +
									"            <resourceType>http://vocabulary.odm2.org/specimentype/corePiece</resourceType>\n" +
									"        </resourceTypes>\n" +
									"        <materialTypes>\n" +
									"            <materialType>http://vocabulary.odm2.org/medium/particulate</materialType>\n" +
									"            <materialType>http://vocabulary.odm2.org/medium/air</materialType>\n" +
									"        </materialTypes>\n" +
									"        <classifications>\n" +
									"            <classification classificationURI=\"http://www.classification.com/tin\">Some phrases for classification</classification>\n" +
									"            <classification classificationURI=\"http://www.classification.com/gold\">maybe gold or silver</classification>\n" +
									"        </classifications>\n" +
									"        <purpose>This is a test resource for demo single</purpose>\n" +
									"        <sampledFeatures>\n" +
									"            <sampledFeature sampledFeatureURI=\"http://www.samplefeature.com/uri/paper\">Paper covers rock</sampledFeature>\n" +
									"            <sampledFeature sampledFeatureURI=\"http://www.samplefeature.com/uri/scisors\">Scissors cuts paper</sampledFeature>\n" +
									"            <sampledFeature sampledFeatureURI=\"http://www.samplefeature.com/uri/rock\">Rock breaks scissors</sampledFeature>\n" +
									"        </sampledFeatures>\n" +
									"        <location>\n" +
									"            <locality localityURI=\"http://google.map/perth\">Canning vale</locality>\n" +
									"            <geometry srid=\"https://epsg.io/4326\" verticalDatum=\"https://epsg.io/4326\" geometryURI=\"http://www.altova.com\">POLYGON ((127.05688476563 -20.5224609375, 124.24438476563 -28.2568359375, 143.22875976563 -32.1240234375, 142.17407226563 -20.8740234375, 127.05688476563 -20.5224609375))</geometry>\n" +
									"        </location>\n" +
									"        <date>\n" +
									"            <timePeriod>\n" +
									"                <start>2003</start>\n" +
									"                <end>2002</end>\n" +
									"            </timePeriod>\n" +
									"        </date>\n" +
									"        <method methodURI=\"http://method.com/collection\">Lab sampling</method>\n" +
									"        <campaign>a</campaign>\n" +
									"        <curationDetails>\n" +
									"            <curation>\n" +
									"                <curator>Curtin</curator>\n" +
									"                <curationDate>2001-12</curationDate>\n" +
									"                <curationLocation>Bentley</curationLocation>\n" +
									"                <curatingInstitution institutionURI=\"http://www.curtin.org\">Curtin University</curatingInstitution>\n" +
									"            </curation>\n" +
									"            <curation>\n" +
									"                <curator>CSIRO</curator>\n" +
									"                <curationDate>2001-12</curationDate>\n" +
									"                <curationLocation>In the lab somewhere</curationLocation>\n" +
									"                <curatingInstitution institutionURI=\"http://csiro.au\">ARRC</curatingInstitution>\n" +
									"            </curation>\n" +
									"        </curationDetails>\n" +
									"        <contributors>\n" +
									"            <contributor contributorType=\"http://registry.it.csiro.au/def/isotc211/CI_RoleCode/pointOfContact\">\n" +
									"                <contributorName>Dwayne Johnsony</contributorName>\n" +
									"                <contributorIdentifier contributorIdentifierType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/ORCID\">WWE0002</contributorIdentifier>\n" +
									"            </contributor>\n" +
									"            <contributor contributorType=\"http://registry.it.csiro.au/def/isotc211/CI_RoleCode/originator\">\n" +
									"                <contributorName>Dwayne Douglas Johnson</contributorName>\n" +
									"                <contributorIdentifier contributorIdentifierType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/EISSN\">WWF0001</contributorIdentifier>\n" +
									"            </contributor>\n" +
									"        </contributors>\n" +
									"        <relatedResources>\n" +
									"            <relatedResource relatedResourceIdentifierType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/ARK\" relationType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/HasDigitalRepresentation\">A related resource somewhere</relatedResource>\n" +
									"            <relatedResource relatedResourceIdentifierType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/DOI\" relationType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/IsMemberOf\">A related resource somewhere</relatedResource>\n" +
									"        </relatedResources>\n" +
									"        <comments>This is a comment section about the rock samples</comments>\n" +
									"        <logDate eventType=\"registered\">2002</logDate>\n" +
									"    </resource>\n" +
									"</resources>")})),
			parameters = { @Parameter(name = "ownerID",
					description = "The UUID of the intended Owner, if the OwnerType value is set to User, this value must be equal to the User's UUID.",
					schema = @Schema(implementation = UUID.class)),
					@Parameter(name = "ownerType", description = "The Type of the Owner",
							schema = @Schema(description = "Owner Type", type = "string",
									allowableValues = { "User", "DataCenter" })) },
			responses = {
					@ApiResponse(responseCode = "200", description = "Mint request is accepted",
							content = @Content(schema = @Schema(implementation = RequestDTO.class))),
					@ApiResponse(responseCode = "403", description = "Forbidden Operation Exception",
							content = @Content(schema = @Schema(implementation = APIExceptionResponse.class))),
					@ApiResponse(responseCode = "400", description = "Validation Exception",
							content = @Content(schema = @Schema(implementation = APIExceptionResponse.class))) })
	public ResponseEntity<RequestDTO> mint(HttpServletRequest httpServletRequest, @RequestBody String payload,
										   @RequestParam(required = false) String ownerID,
										   @RequestParam(required = false, defaultValue = "User") String ownerType) throws Exception {
		User user = keycloakService.getLoggedInUser(httpServletRequest);

		// creating the IGSN Request & write the payload to file
		Request request = igsnRequestService.createRequest(user, IGSNService.EVENT_MINT, payload);
		httpServletRequest.setAttribute(String.valueOf(Request.class), request);

		request.setAttribute(Attribute.OWNER_TYPE, ownerType);
		request.setAttribute(Attribute.CREATOR_ID, user.getId().toString());
		if (ownerID != null) {
			request.setAttribute(Attribute.OWNER_ID, ownerID);
		}
		else {
			request.setAttribute(Attribute.OWNER_ID, user.getId().toString());
		}

		// Validate the request
		igsnRequestValidationService.validate(request, user);
		// process (single)
		request.setAttribute(Attribute.NUM_OF_RECORDS_RECEIVED, "1");
		igsnRequestService.save(request);

		// run
		request.setStatus(Request.Status.RUNNING);

		String dataPath = request.getAttribute(Attribute.DATA_PATH);
		String schemaID = request.getAttribute(Attribute.SCHEMA_ID);
		FragmentProvider fragmentProvider = (FragmentProvider) MetadataProviderFactory
				.create(schemaService.getSchemaByID(schemaID), Metadata.Fragment);
		// get the fragment so we are in control of the container
		String content = fragmentProvider.get(payload, 0);
		String fragmentPath = dataPath + File.separator + "fragment.xml";
		Helpers.writeFile(fragmentPath, content);
		IdentifierProvider identifierProvider = (IdentifierProvider) MetadataProviderFactory
				.create(schemaService.getSchemaByID(schemaID), Metadata.Identifier);
		String identifierValue = identifierProvider.get(payload, 0);
		ImportIGSNTask task = new ImportIGSNTask(identifierValue, new File(fragmentPath), request, importService,
				applicationEventPublisher, igsnRequestService);
		task.run();

		// finish request
		igsnService.finalizeRequest(request);

		RequestDTO dto = requestMapper.getConverter().convert(request);
		return ResponseEntity.ok().body(dto);
	}

	/**
	 * Update IGSN Service endpoint.
	 * @param httpServletRequest the {@link HttpServletRequest} for this request
	 * @param payload the required {@link RequestBody} for this request
	 * @return an IGSN response records
	 * @throws Exception when things go wrong, handled by Exception Advice
	 */
	@PostMapping(value = "/update", consumes = { MediaType.APPLICATION_XML_VALUE})
	@Operation(summary = "Update IGSN", description = "Updates an existing IGSN metadata",
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
					description = "the updated descriptive metadata of a single IGSN record up to 60KB",
					content= @Content(examples = {
							@ExampleObject(name="ARDC v1 XML", value="<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
									"<resources xmlns=\"https://identifiers.ardc.edu.au/schemas/ardc-igsn-desc\">\n" +
									"    <resource registeredObjectType=\"http://pid.geoscience.gov.au/def/voc/ga/igsncode/PhysicalSample\">\n" +
									"        <resourceIdentifier>20.500.11812/XXZT10000001</resourceIdentifier>\n" +
									"        <landingPage>https://test.identifiers.ardc.edu.au/igsn-portal/view/20.500.11812/XXZT10000001</landingPage>\n" +
									"        <isPublic>true</isPublic>\n" +
									"        <resourceTitle>An Updated Record Rock sample (Sample JBS35 / IGSN XXZT10000001)</resourceTitle>\n" +
									"        <resourceTypes>\n" +
									"            <resourceType>http://vocabulary.odm2.org/specimentype/other</resourceType>\n" +
									"        </resourceTypes>\n" +
									"        <materialTypes>\n" +
									"            <materialType>http://vocabulary.odm2.org/medium/mineral</materialType>\n" +
									"        </materialTypes>\n" +
									"        <classifications>\n" +
									"            <classification>igneous granitic (granite)</classification>\n" +
									"        </classifications>\n" +
									"        <sampledFeatures>\n" +
									"            <sampledFeature>Sholl Belt</sampledFeature>\n" +
									"        </sampledFeatures>\n" +
									"        <location>\n" +
									"            <locality>2km inside the south margin of the granitoid</locality>\n" +
									"            <geometry srid=\"https://epsg.io/4326\">POINT (116.851 -20.829)</geometry>\n" +
									"        </location>\n" +
									"        <curationDetails>\n" +
									"            <curation>\n" +
									"                <curator>\n" +
									"                    <curatorName>Geological Survey of Western Australia</curatorName>\n" +
									"                </curator>\n" +
									"                <curationDate>2019-06-21</curationDate>\n" +
									"                <curationLocation>Bentley</curationLocation>\n" +
									"                <curatingInstitution institutionURI=\"http://www.curtin.edu.au\">Curtin University</curatingInstitution>\n" +
									"            </curation>\n" +
									"        </curationDetails>\n" +
									"        <contributors>\n" +
									"            <contributor contributorType=\"http://registry.it.csiro.au/def/isotc211/CI_RoleCode/pointOfContact\">\n" +
									"                <contributorName>Dwayne Douglas Johnson</contributorName>\n" +
									"            </contributor>\n" +
									"        </contributors>\n" +
									"        <comments>Data has been provided by the Dwayne Johnson Collection.</comments>\n" +
									"        <logDate eventType=\"registered\">1995</logDate>\n" +
									"    </resource>\n" +
									"</resources>"),
							@ExampleObject(name="CSIRO v3 XML", value="<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
									"<resources xmlns=\"https://igsn.csiro.au/schemas/3.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"https://igsn.csiro.au/schemas/3.0 https://igsn.csiro.au/schemas/3.0/igsn-csiro-v3.0.xsd\">\n" +
									"    <resource registeredObjectType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/PhysicalSample\">\n" +
									"        <resourceIdentifier>XXZT10000001</resourceIdentifier>\n" +
									"        <landingPage>https://test.identifiers.ardc.edu.au/igsn-portal/view/20.500.11812/XXZT10000001</landingPage>\n" +
									"        <isPublic>true</isPublic>\n" +
									"        <resourceTitle>An Updated CSIRO v3 sample rock sample from the Dwayne Johnsony Collection</resourceTitle>\n" +
									"        <alternateIdentifiers>\n" +
									"            <alternateIdentifier>An alternative Identifier</alternateIdentifier>\n" +
									"            <alternateIdentifier>A second alternative Identifer</alternateIdentifier>\n" +
									"        </alternateIdentifiers>\n" +
									"        <resourceTypes>\n" +
									"            <resourceType>http://vocabulary.odm2.org/specimentype/core</resourceType>\n" +
									"            <resourceType>http://vocabulary.odm2.org/specimentype/corePiece</resourceType>\n" +
									"        </resourceTypes>\n" +
									"        <materialTypes>\n" +
									"            <materialType>http://vocabulary.odm2.org/medium/particulate</materialType>\n" +
									"            <materialType>http://vocabulary.odm2.org/medium/air</materialType>\n" +
									"        </materialTypes>\n" +
									"        <classifications>\n" +
									"            <classification classificationURI=\"http://www.classification.com/tin\">Some phrases for classification</classification>\n" +
									"            <classification classificationURI=\"http://www.classification.com/gold\">maybe gold or silver</classification>\n" +
									"        </classifications>\n" +
									"        <purpose>This is a test resource for demo single</purpose>\n" +
									"        <sampledFeatures>\n" +
									"            <sampledFeature sampledFeatureURI=\"http://www.samplefeature.com/uri/paper\">Paper covers rock</sampledFeature>\n" +
									"            <sampledFeature sampledFeatureURI=\"http://www.samplefeature.com/uri/scisors\">Scissors cuts paper</sampledFeature>\n" +
									"            <sampledFeature sampledFeatureURI=\"http://www.samplefeature.com/uri/rock\">Rock breaks scissors</sampledFeature>\n" +
									"        </sampledFeatures>\n" +
									"        <location>\n" +
									"            <locality localityURI=\"http://google.map/perth\">Canning vale</locality>\n" +
									"            <geometry srid=\"https://epsg.io/4326\" verticalDatum=\"https://epsg.io/4326\" geometryURI=\"http://www.altova.com\">POLYGON ((127.05688476563 -20.5224609375, 124.24438476563 -28.2568359375, 143.22875976563 -32.1240234375, 142.17407226563 -20.8740234375, 127.05688476563 -20.5224609375))</geometry>\n" +
									"        </location>\n" +
									"        <date>\n" +
									"            <timePeriod>\n" +
									"                <start>2003</start>\n" +
									"                <end>2002</end>\n" +
									"            </timePeriod>\n" +
									"        </date>\n" +
									"        <method methodURI=\"http://method.com/collection\">Lab sampling</method>\n" +
									"        <campaign>a</campaign>\n" +
									"        <curationDetails>\n" +
									"            <curation>\n" +
									"                <curator>Curtin</curator>\n" +
									"                <curationDate>2001-12</curationDate>\n" +
									"                <curationLocation>Bentley</curationLocation>\n" +
									"                <curatingInstitution institutionURI=\"http://www.curtin.org\">Curtin University</curatingInstitution>\n" +
									"            </curation>\n" +
									"            <curation>\n" +
									"                <curator>CSIRO</curator>\n" +
									"                <curationDate>2001-12</curationDate>\n" +
									"                <curationLocation>In the lab somewhere</curationLocation>\n" +
									"                <curatingInstitution institutionURI=\"http://csiro.au\">ARRC</curatingInstitution>\n" +
									"            </curation>\n" +
									"        </curationDetails>\n" +
									"        <contributors>\n" +
									"            <contributor contributorType=\"http://registry.it.csiro.au/def/isotc211/CI_RoleCode/pointOfContact\">\n" +
									"                <contributorName>Dwayne Johnsony</contributorName>\n" +
									"                <contributorIdentifier contributorIdentifierType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/ORCID\">WWE0002</contributorIdentifier>\n" +
									"            </contributor>\n" +
									"            <contributor contributorType=\"http://registry.it.csiro.au/def/isotc211/CI_RoleCode/originator\">\n" +
									"                <contributorName>Dwayne Douglas Johnson</contributorName>\n" +
									"                <contributorIdentifier contributorIdentifierType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/EISSN\">WWF0001</contributorIdentifier>\n" +
									"            </contributor>\n" +
									"        </contributors>\n" +
									"        <relatedResources>\n" +
									"            <relatedResource relatedResourceIdentifierType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/ARK\" relationType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/HasDigitalRepresentation\">A related resource somewhere</relatedResource>\n" +
									"            <relatedResource relatedResourceIdentifierType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/DOI\" relationType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/IsMemberOf\">A related resource somewhere</relatedResource>\n" +
									"        </relatedResources>\n" +
									"        <comments>This is a comment section about the rock samples</comments>\n" +
									"        <logDate eventType=\"registered\">2002</logDate>\n" +
									"    </resource>\n" +
									"</resources>")})),
			responses = {
					@ApiResponse(responseCode = "200", description = "Update request is accepted",
							content = @Content(schema = @Schema(implementation = RequestDTO.class))),
					@ApiResponse(responseCode = "403", description = "Forbidden Operation Exception",
							content = @Content(schema = @Schema(implementation = APIExceptionResponse.class))),
					@ApiResponse(responseCode = "400", description = "Validation Exception",
							content = @Content(schema = @Schema(implementation = APIExceptionResponse.class))) })
	public ResponseEntity<RequestDTO> update(HttpServletRequest httpServletRequest, @RequestBody String payload)
			throws Exception {
		User user = keycloakService.getLoggedInUser(httpServletRequest);

		// creating the IGSN Request & write the payload to file
		Request request = igsnRequestService.createRequest(user, IGSNService.EVENT_UPDATE, payload);
		httpServletRequest.setAttribute(String.valueOf(Request.class), request);

		// Validate the request
		igsnRequestValidationService.validate(request, user);
		request.setStatus(Request.Status.ACCEPTED);
		igsnRequestService.save(request);

		// process
		request.setAttribute(Attribute.CREATOR_ID, user.getId().toString());
		request.setAttribute(Attribute.NUM_OF_RECORDS_RECEIVED, "1");
		igsnRequestService.save(request);

		// run
		request.setStatus(Request.Status.RUNNING);

		String dataPath = request.getAttribute(Attribute.DATA_PATH);
		String schemaID = request.getAttribute(Attribute.SCHEMA_ID);
		FragmentProvider fragmentProvider = (FragmentProvider) MetadataProviderFactory
				.create(schemaService.getSchemaByID(schemaID), Metadata.Fragment);
		// use the new and improved content
		String content = fragmentProvider.get(payload, 0);
		String fragmentPath = dataPath + File.separator + "fragment.xml";

		Helpers.writeFile(fragmentPath, content);
		IdentifierProvider identifierProvider = (IdentifierProvider) MetadataProviderFactory
				.create(schemaService.getSchemaByID(schemaID), Metadata.Identifier);
		String identifierValue = identifierProvider.get(payload, 0);
		UpdateIGSNTask task = new UpdateIGSNTask(identifierValue, new File(fragmentPath), request, importService,
				applicationEventPublisher, igsnRequestService);
		task.run();

		// finish
		igsnService.finalizeRequest(request);

		RequestDTO dto = requestMapper.getConverter().convert(request);
		return ResponseEntity.ok().body(dto);
	}

	@PostMapping(value = "/bulk-update", consumes = { MediaType.APPLICATION_XML_VALUE})
	@Operation(summary = "Bulk Update IGSN", description = "Updates many IGSNs metadata in a single payload",
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
					description = "The updated XML descriptive metadata of up to 1000 records or 5MB in a single XML payload",
					content= @Content(examples = {
							@ExampleObject(name="ARDC v1 XML", value="<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
									"<resources xmlns=\"https://identifiers.ardc.edu.au/schemas/ardc-igsn-desc\">\n" +
									"    <resource registeredObjectType=\"http://pid.geoscience.gov.au/def/voc/ga/igsncode/PhysicalSample\">\n" +
									"        <resourceIdentifier>20.500.11812/XXZT10000001</resourceIdentifier>\n" +
									"        <landingPage>https://test.identifiers.ardc.edu.au/igsn-portal/view/20.500.11812/XXZT10000001</landingPage>\n" +
									"        <isPublic>true</isPublic>\n" +
									"        <resourceTitle>An UPDATED title Rock sample (Sample JBS35 / IGSN XXZT10000001)</resourceTitle>\n" +
									"        <resourceTypes>\n" +
									"            <resourceType>http://vocabulary.odm2.org/specimentype/other</resourceType>\n" +
									"        </resourceTypes>\n" +
									"        <materialTypes>\n" +
									"            <materialType>http://vocabulary.odm2.org/medium/mineral</materialType>\n" +
									"        </materialTypes>\n" +
									"        <classifications>\n" +
									"            <classification>igneous granitic (granite)</classification>\n" +
									"        </classifications>\n" +
									"        <sampledFeatures>\n" +
									"            <sampledFeature>Sholl Belt</sampledFeature>\n" +
									"        </sampledFeatures>\n" +
									"        <location>\n" +
									"            <locality>2km inside the south margin of the granitoid</locality>\n" +
									"            <geometry srid=\"https://epsg.io/4326\">POINT (116.851 -20.829)</geometry>\n" +
									"        </location>\n" +
									"        <curationDetails>\n" +
									"            <curation>\n" +
									"                <curator>\n" +
									"                    <curatorName>Geological Survey of Western Australia</curatorName>\n" +
									"                </curator>\n" +
									"                <curationDate>2019-06-21</curationDate>\n" +
									"                <curationLocation>Bentley</curationLocation>\n" +
									"                <curatingInstitution institutionURI=\"http://www.curtin.edu.au\">Curtin University</curatingInstitution>\n" +
									"            </curation>\n" +
									"        </curationDetails>\n" +
									"        <contributors>\n" +
									"            <contributor contributorType=\"http://registry.it.csiro.au/def/isotc211/CI_RoleCode/pointOfContact\">\n" +
									"                <contributorName>Dwayne Douglas Johnson</contributorName>\n" +
									"            </contributor>\n" +
									"        </contributors>\n" +
									"        <comments>Data has been provided by the Dwayne Johnson Collection.</comments>\n" +
									"        <logDate eventType=\"registered\">1995</logDate>\n" +
									"    </resource>\n" +
									"        <resource registeredObjectType=\"http://pid.geoscience.gov.au/def/voc/ga/igsncode/PhysicalSample\">\n" +
									"        <resourceIdentifier>20.500.11812/XXZT10000002</resourceIdentifier>\n" +
									"        <landingPage>https://test.identifiers.ardc.edu.au/igsn-portal/view/20.500.11812/XXZT10000002</landingPage>\n" +
									"        <isPublic>true</isPublic>\n" +
									"        <resourceTitle>A second Rock sample (Sample JBS34 / IGSN XXZT10000002)</resourceTitle>\n" +
									"        <resourceTypes>\n" +
									"            <resourceType>http://vocabulary.odm2.org/specimentype/other</resourceType>\n" +
									"        </resourceTypes>\n" +
									"        <materialTypes>\n" +
									"            <materialType>http://vocabulary.odm2.org/medium/mineral</materialType>\n" +
									"        </materialTypes>\n" +
									"        <classifications>\n" +
									"            <classification>igneous granitic (granite)</classification>\n" +
									"        </classifications>\n" +
									"        <sampledFeatures>\n" +
									"            <sampledFeature>Sholl Belt</sampledFeature>\n" +
									"        </sampledFeatures>\n" +
									"        <location>\n" +
									"            <locality>2km inside the south margin of the granitoid</locality>\n" +
									"            <geometry srid=\"https://epsg.io/4326\">POINT (116.851 -20.829)</geometry>\n" +
									"        </location>\n" +
									"        <curationDetails>\n" +
									"            <curation>\n" +
									"                <curator>\n" +
									"                    <curatorName>Geological Survey of Western Australia</curatorName>\n" +
									"                </curator>\n" +
									"                <curationDate>2019-06-21</curationDate>\n" +
									"                <curationLocation>Bentley</curationLocation>\n" +
									"                <curatingInstitution institutionURI=\"http://www.curtin.edu.au\">Curtin University</curatingInstitution>\n" +
									"            </curation>\n" +
									"        </curationDetails>\n" +
									"        <contributors>\n" +
									"            <contributor contributorType=\"http://registry.it.csiro.au/def/isotc211/CI_RoleCode/pointOfContact\">\n" +
									"                <contributorName>Dwayne Douglas Johnson</contributorName>\n" +
									"            </contributor>\n" +
									"        </contributors>\n" +
									"        <comments>Updated Comment Data has been provided by the Dwayne Johnson Collection.</comments>\n" +
									"        <logDate eventType=\"registered\">1995</logDate>\n" +
									"    </resource>\n" +
									"</resources>"),
							@ExampleObject(name="CSIRO v3 XML", value="<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
									"<resources xmlns=\"https://igsn.csiro.au/schemas/3.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"https://igsn.csiro.au/schemas/3.0 https://igsn.csiro.au/schemas/3.0/igsn-csiro-v3.0.xsd\">\n" +
									"    <resource registeredObjectType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/PhysicalSample\">\n" +
									"        <resourceIdentifier>XXZT10000001</resourceIdentifier>\n" +
									"        <landingPage>https://test.identifiers.ardc.edu.au/igsn-portal/view/20.500.11812/XXZT10000001</landingPage>\n" +
									"        <isPublic>true</isPublic>\n" +
									"        <resourceTitle>An UPDATED title rock sample from the Dwayne Johnsony Collection</resourceTitle>\n" +
									"        <alternateIdentifiers>\n" +
									"            <alternateIdentifier>An alternative Identifier</alternateIdentifier>\n" +
									"            <alternateIdentifier>A second alternative Identifer</alternateIdentifier>\n" +
									"        </alternateIdentifiers>\n" +
									"        <resourceTypes>\n" +
									"            <resourceType>http://vocabulary.odm2.org/specimentype/core</resourceType>\n" +
									"            <resourceType>http://vocabulary.odm2.org/specimentype/corePiece</resourceType>\n" +
									"        </resourceTypes>\n" +
									"        <materialTypes>\n" +
									"            <materialType>http://vocabulary.odm2.org/medium/particulate</materialType>\n" +
									"            <materialType>http://vocabulary.odm2.org/medium/air</materialType>\n" +
									"        </materialTypes>\n" +
									"        <classifications>\n" +
									"            <classification classificationURI=\"http://www.classification.com/tin\">Some phrases for classification</classification>\n" +
									"            <classification classificationURI=\"http://www.classification.com/gold\">maybe gold or silver</classification>\n" +
									"        </classifications>\n" +
									"        <purpose>This is a test resource for demo single</purpose>\n" +
									"        <sampledFeatures>\n" +
									"            <sampledFeature sampledFeatureURI=\"http://www.samplefeature.com/uri/paper\">Paper covers rock</sampledFeature>\n" +
									"            <sampledFeature sampledFeatureURI=\"http://www.samplefeature.com/uri/scisors\">Scissors cuts paper</sampledFeature>\n" +
									"            <sampledFeature sampledFeatureURI=\"http://www.samplefeature.com/uri/rock\">Rock breaks scissors</sampledFeature>\n" +
									"        </sampledFeatures>\n" +
									"        <location>\n" +
									"            <locality localityURI=\"http://google.map/perth\">Canning vale</locality>\n" +
									"            <geometry srid=\"https://epsg.io/4326\" verticalDatum=\"https://epsg.io/4326\" geometryURI=\"http://www.altova.com\">POLYGON ((127.05688476563 -20.5224609375, 124.24438476563 -28.2568359375, 143.22875976563 -32.1240234375, 142.17407226563 -20.8740234375, 127.05688476563 -20.5224609375))</geometry>\n" +
									"        </location>\n" +
									"        <date>\n" +
									"            <timePeriod>\n" +
									"                <start>2003</start>\n" +
									"                <end>2002</end>\n" +
									"            </timePeriod>\n" +
									"        </date>\n" +
									"        <method methodURI=\"http://method.com/collection\">Lab sampling</method>\n" +
									"        <campaign>a</campaign>\n" +
									"        <curationDetails>\n" +
									"            <curation>\n" +
									"                <curator>Curtin</curator>\n" +
									"                <curationDate>2001-12</curationDate>\n" +
									"                <curationLocation>Bentley</curationLocation>\n" +
									"                <curatingInstitution institutionURI=\"http://www.curtin.org\">Curtin University</curatingInstitution>\n" +
									"            </curation>\n" +
									"            <curation>\n" +
									"                <curator>CSIRO</curator>\n" +
									"                <curationDate>2001-12</curationDate>\n" +
									"                <curationLocation>In the lab somewhere</curationLocation>\n" +
									"                <curatingInstitution institutionURI=\"http://csiro.au\">ARRC</curatingInstitution>\n" +
									"            </curation>\n" +
									"        </curationDetails>\n" +
									"        <contributors>\n" +
									"            <contributor contributorType=\"http://registry.it.csiro.au/def/isotc211/CI_RoleCode/pointOfContact\">\n" +
									"                <contributorName>Dwayne Johnsony</contributorName>\n" +
									"                <contributorIdentifier contributorIdentifierType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/ORCID\">WWE0002</contributorIdentifier>\n" +
									"            </contributor>\n" +
									"            <contributor contributorType=\"http://registry.it.csiro.au/def/isotc211/CI_RoleCode/originator\">\n" +
									"                <contributorName>Dwayne Douglas Johnson</contributorName>\n" +
									"                <contributorIdentifier contributorIdentifierType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/EISSN\">WWF0001</contributorIdentifier>\n" +
									"            </contributor>\n" +
									"        </contributors>\n" +
									"        <relatedResources>\n" +
									"            <relatedResource relatedResourceIdentifierType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/ARK\" relationType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/HasDigitalRepresentation\">A related resource somewhere</relatedResource>\n" +
									"            <relatedResource relatedResourceIdentifierType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/DOI\" relationType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/IsMemberOf\">A related resource somewhere</relatedResource>\n" +
									"        </relatedResources>\n" +
									"        <comments>This is a comment section about the rock samples</comments>\n" +
									"        <logDate eventType=\"registered\">2002</logDate>\n" +
									"    </resource>\n" +
									"    <resource registeredObjectType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/PhysicalSample\">\n" +
									"        <resourceIdentifier>XXZT10000002</resourceIdentifier>\n" +
									"        <landingPage>https://test.identifiers.ardc.edu.au/igsn-portal/view/20.500.11812/XXZT10000002</landingPage>\n" +
									"        <isPublic>true</isPublic>\n" +
									"        <resourceTitle>A Second rock sample from the Dwayne Johnsony Collection</resourceTitle>\n" +
									"        <alternateIdentifiers>\n" +
									"            <alternateIdentifier>An alternative Identifier</alternateIdentifier>\n" +
									"            <alternateIdentifier>A second alternative Identifer</alternateIdentifier>\n" +
									"        </alternateIdentifiers>\n" +
									"        <resourceTypes>\n" +
									"            <resourceType>http://vocabulary.odm2.org/specimentype/core</resourceType>\n" +
									"            <resourceType>http://vocabulary.odm2.org/specimentype/corePiece</resourceType>\n" +
									"        </resourceTypes>\n" +
									"        <materialTypes>\n" +
									"            <materialType>http://vocabulary.odm2.org/medium/particulate</materialType>\n" +
									"            <materialType>http://vocabulary.odm2.org/medium/air</materialType>\n" +
									"        </materialTypes>\n" +
									"        <classifications>\n" +
									"            <classification classificationURI=\"http://www.classification.com/tin\">Some phrases for classification</classification>\n" +
									"            <classification classificationURI=\"http://www.classification.com/gold\">maybe gold or silver</classification>\n" +
									"        </classifications>\n" +
									"        <purpose>This is a test resource for demo single</purpose>\n" +
									"        <sampledFeatures>\n" +
									"            <sampledFeature sampledFeatureURI=\"http://www.samplefeature.com/uri/paper\">Paper covers rock</sampledFeature>\n" +
									"            <sampledFeature sampledFeatureURI=\"http://www.samplefeature.com/uri/scisors\">Scissors cuts paper</sampledFeature>\n" +
									"            <sampledFeature sampledFeatureURI=\"http://www.samplefeature.com/uri/rock\">Rock breaks scissors</sampledFeature>\n" +
									"        </sampledFeatures>\n" +
									"        <location>\n" +
									"            <locality localityURI=\"http://google.map/perth\">Canning vale</locality>\n" +
									"            <geometry srid=\"https://epsg.io/4326\" verticalDatum=\"https://epsg.io/4326\" geometryURI=\"http://www.altova.com\">POLYGON ((127.05688476563 -20.5224609375, 124.24438476563 -28.2568359375, 143.22875976563 -32.1240234375, 142.17407226563 -20.8740234375, 127.05688476563 -20.5224609375))</geometry>\n" +
									"        </location>\n" +
									"        <date>\n" +
									"            <timePeriod>\n" +
									"                <start>2003</start>\n" +
									"                <end>2002</end>\n" +
									"            </timePeriod>\n" +
									"        </date>\n" +
									"        <method methodURI=\"http://method.com/collection\">Lab sampling</method>\n" +
									"        <campaign>a</campaign>\n" +
									"        <curationDetails>\n" +
									"            <curation>\n" +
									"                <curator>Curtin</curator>\n" +
									"                <curationDate>2001-12</curationDate>\n" +
									"                <curationLocation>Bentley</curationLocation>\n" +
									"                <curatingInstitution institutionURI=\"http://www.curtin.org\">Curtin University</curatingInstitution>\n" +
									"            </curation>\n" +
									"            <curation>\n" +
									"                <curator>CSIRO</curator>\n" +
									"                <curationDate>2001-12</curationDate>\n" +
									"                <curationLocation>In the lab somewhere</curationLocation>\n" +
									"                <curatingInstitution institutionURI=\"http://csiro.au\">ARRC</curatingInstitution>\n" +
									"            </curation>\n" +
									"        </curationDetails>\n" +
									"        <contributors>\n" +
									"            <contributor contributorType=\"http://registry.it.csiro.au/def/isotc211/CI_RoleCode/pointOfContact\">\n" +
									"                <contributorName>Dwayne Johnsony</contributorName>\n" +
									"                <contributorIdentifier contributorIdentifierType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/ORCID\">WWE0002</contributorIdentifier>\n" +
									"            </contributor>\n" +
									"            <contributor contributorType=\"http://registry.it.csiro.au/def/isotc211/CI_RoleCode/originator\">\n" +
									"                <contributorName>Dwayne Douglas Johnson</contributorName>\n" +
									"                <contributorIdentifier contributorIdentifierType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/EISSN\">WWF0001</contributorIdentifier>\n" +
									"            </contributor>\n" +
									"        </contributors>\n" +
									"        <relatedResources>\n" +
									"            <relatedResource relatedResourceIdentifierType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/ARK\" relationType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/HasDigitalRepresentation\">A related resource somewhere</relatedResource>\n" +
									"            <relatedResource relatedResourceIdentifierType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/DOI\" relationType=\"http://pid.geoscience.gov.au/def/voc/igsn-codelists/IsMemberOf\">A related resource somewhere</relatedResource>\n" +
									"        </relatedResources>\n" +
									"        <comments>This is an updated comment section about the rock samples</comments>\n" +
									"        <logDate eventType=\"registered\">2002</logDate>\n" +
									"    </resource>\n" +
									"</resources>")})),
			responses = {
					@ApiResponse(responseCode = "200", description = "Bulk Update request is accepted",
							content = @Content(schema = @Schema(implementation = RequestDTO.class))),
					@ApiResponse(responseCode = "403", description = "Forbidden Operation Exception",
							content = @Content(schema = @Schema(implementation = APIExceptionResponse.class))),
					@ApiResponse(responseCode = "400", description = "Validation Exception",
							content = @Content(schema = @Schema(implementation = APIExceptionResponse.class))) })
	public ResponseEntity<RequestDTO> bulkUpdate(HttpServletRequest httpServletRequest, @RequestBody String payload)
			throws Exception {
		User user = keycloakService.getLoggedInUser(httpServletRequest);

		// creating the IGSN Request & write the payload to file
		Request request = igsnRequestService.createRequest(user, IGSNService.EVENT_BULK_UPDATE, payload);
		httpServletRequest.setAttribute(String.valueOf(Request.class), request);

		// Validate the request
		igsnRequestValidationService.validate(request, user);
		request.setStatus(Request.Status.ACCEPTED);
		igsnRequestService.save(request);

		// the requestvalidator is setting the Allocation ID
		// IGSNAllocation allocation = igsnService.getIGSNAllocationForContent(payload,
		// user, Scope.UPDATE);
		// request.setAttribute(Attribute.ALLOCATION_ID, allocation.getId().toString());
		request.setAttribute(Attribute.CREATOR_ID, user.getId().toString());

		request.setStatus(Request.Status.QUEUED);
		request.setMessage("Bulk Update Request is Queued");
		igsnRequestService.save(request);
		igsnService.processMintOrUpdate(request);

		RequestDTO dto = requestMapper.getConverter().convert(request);
		return ResponseEntity.ok().body(dto);
	}

	@PostMapping(value = "/reserve", consumes = { MediaType.TEXT_PLAIN_VALUE })
	@Operation(summary = "Reserve IGSN", description = "Reserve a list of IGSNs without registering metadata",
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
					description = "the newline separated IGSN list, 1 per line",
					content= @Content(examples = @ExampleObject(value="20.500.11812/XXZT1UDZY8RKYBE234\n" +
							"20.500.11812/XXZT1ECT9BUJ59F234\n" +
							"20.500.11812/XXZT1AXSALZL2GQ234\n" +
							"20.500.11812/XXZT1KK6S8VQEKD234\n" +
							"20.500.11812/XXZT1VDOHF93EPI234\n" +
							"20.500.11812/XXZT1XHURL3OTV5234\n" +
							"20.500.11812/XXZT1RW7GAGZBNH234\n" +
							"20.500.11812/XXZT1ZM34HZOG7V234\n" +
							"20.500.11812/XXZT1FZTQSKVIP7234\n" +
							"20.500.11812/XXZT16FDGYYH1XR234"))),
			parameters = {
					@Parameter(name = "schemaID", description = "the schema of the payload",
							schema = @Schema(description = "Schema ID", type = "string",
									allowableValues = { "igsn_list" }, defaultValue = "igsn_list")),
					@Parameter(name = "ownerID",
							description = "The UUID of the intended Owner, if the OwnerType value is set to User, this value must be equal to the User's UUID.",
							schema = @Schema(implementation = UUID.class)),
					@Parameter(name = "ownerType", description = "The Type of the Owner",
							schema = @Schema(description = "Owner Type", type = "string",
									allowableValues = { "User", "DataCenter" })) },
			responses = {
					@ApiResponse(responseCode = "200", description = "Reserve request has completed successfully",
							content = @Content(schema = @Schema(implementation = RequestDTO.class))),
					@ApiResponse(responseCode = "403", description = "Forbidden Operation Exception",
							content = @Content(schema = @Schema(implementation = APIExceptionResponse.class))),
					@ApiResponse(responseCode = "400", description = "Validation Exception",
							content = @Content(schema = @Schema(implementation = APIExceptionResponse.class))) })
	public ResponseEntity<RequestDTO> reserve(HttpServletRequest httpServletRequest,
											  @RequestParam(required = false, defaultValue = "igsn_list") String schemaID,
											  @RequestParam(required = false, defaultValue = "User") String ownerType,
											  @RequestParam(required = false) String ownerID, @RequestBody String payload) throws IOException {
		User user = keycloakService.getLoggedInUser(httpServletRequest);
		Request request = igsnRequestService.createRequest(user, IGSNService.EVENT_RESERVE, payload);
		httpServletRequest.setAttribute(String.valueOf(Request.class), request);
		request.setAttribute(Attribute.SCHEMA_ID, schemaID);
		request.setAttribute(Attribute.OWNER_TYPE, ownerType);
		request.setAttribute(Attribute.CREATOR_ID, user.getId().toString());
		if (ownerID != null) {
			request.setAttribute(Attribute.OWNER_ID, ownerID);
		}
		else {
			request.setAttribute(Attribute.OWNER_ID, user.getId().toString());
		}
		// todo validate the request
		igsnRequestValidationService.validate(request, user);
		request.setStatus(Request.Status.QUEUED);
		request.setMessage("Bulk Reserve Request is Queued");
		igsnRequestService.save(request);
		igsnService.processReserve(request);

		RequestDTO dto = requestMapper.getConverter().convert(request);
		return ResponseEntity.ok().body(dto);
	}

	@PostMapping(value = "/transfer", consumes = MediaType.TEXT_PLAIN_VALUE)
	@Operation(summary = "Transfer IGSN ownership",
			description = "Transfer the ownership of a list of IGSN Records to a DataCenter the User is member of",
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
					description = "A list of Identifiers owned by the user, 1 IGSN per line",
//				 content= @Content(
//				 examples = @ExampleObject(name="IGSN list",
//				 externalValue = "https://test.identifiers.ardc.edu.au/igsn-registry/examples/igsnlist.txt"))),
					content= @Content(examples = @ExampleObject(value="20.500.11812/XXZT1UDZY8RKYBE234\n" +
							"20.500.11812/XXZT1ECT9BUJ59F234\n" +
							"20.500.11812/XXZT1AXSALZL2GQ234\n" +
							"20.500.11812/XXZT1KK6S8VQEKD234\n" +
							"20.500.11812/XXZT1VDOHF93EPI234\n" +
							"20.500.11812/XXZT1XHURL3OTV5234\n" +
							"20.500.11812/XXZT1RW7GAGZBNH234\n" +
							"20.500.11812/XXZT1ZM34HZOG7V234\n" +
							"20.500.11812/XXZT1FZTQSKVIP7234\n" +
							"20.500.11812/XXZT16FDGYYH1XR234"))),
			parameters = {
					@Parameter(name = "schemaID", description = "the schema of the payload",
							schema = @Schema(description = "Schema ID", type = "string",
									allowableValues = { "igsn_list" }, defaultValue = "igsn_list")),
					@Parameter(name = "ownerID", required = true, description = "The UUID of the user's DataCenter to transfer Ownership to",
							schema = @Schema(implementation = UUID.class)),
					@Parameter(name = "ownerType", description = "The Type of the Owner currently only DataCenter is supported",
							schema = @Schema(description = "Owner Type", type = "string", defaultValue = "DataCenter",
									allowableValues = {"DataCenter"})) },
			responses = {
					@ApiResponse(responseCode = "200", description = "Transfer request has completed successfully",
							content = @Content(schema = @Schema(implementation = RequestDTO.class))),
					@ApiResponse(responseCode = "403", description = "Forbidden Operation Exception",
							content = @Content(schema = @Schema(implementation = APIExceptionResponse.class))),
					@ApiResponse(responseCode = "400", description = "Validation Exception",
							content = @Content(schema = @Schema(implementation = APIExceptionResponse.class))) })
	public ResponseEntity<RequestDTO> transfer(HttpServletRequest httpServletRequest,
											   @NotNull @RequestParam String ownerID,
											   @RequestParam(required = false, defaultValue = "igsn_list") String schemaID,
											   @RequestParam(required = false, defaultValue = "DataCenter") String ownerType, @RequestBody String payload)
			throws IOException {
		User user = keycloakService.getLoggedInUser(httpServletRequest);

		Request request = igsnRequestService.createRequest(user, IGSNService.EVENT_TRANSFER, payload);
		httpServletRequest.setAttribute(String.valueOf(Request.class), request);

		request.setAttribute(Attribute.SCHEMA_ID, schemaID);
		request.setAttribute(Attribute.OWNER_TYPE, ownerType);
		request.setAttribute(Attribute.CREATOR_ID, user.getId().toString());
		request.setAttribute(Attribute.OWNER_ID, ownerID);

		igsnRequestValidationService.validate(request, user);

		// process

		request.setStatus(Request.Status.QUEUED);
		request.setMessage("Bulk Transfer Request is Queued");
		igsnRequestService.save(request);

		igsnService.processTransfer(request);

		RequestDTO dto = requestMapper.getConverter().convert(request);
		return ResponseEntity.ok().body(dto);
	}

	@GetMapping("/generate-igsn")
	@Operation(summary = "Generate unique IGSN (this is not a mint)",
			description = "Returns an IGSN value ready to be minted. The IGSN generated is not minted and serve only to provide a unique identifier",
			parameters = { @Parameter(name = "allocationID", required = false,
					description = "The allocationID to generate the IGSN for, if blank will default to the first Allocation",
					schema = @Schema(implementation = UUID.class)), },
			responses = { @ApiResponse(responseCode = "200", description = "IGSN value generated",
					content = @Content(schema = @Schema(implementation = RequestDTO.class))), })
	public ResponseEntity<?> generateIGSN(HttpServletRequest httpServletRequest,
										  @RequestParam(required = false) String allocationID) {
		User user = keycloakService.getLoggedInUser(httpServletRequest);

		// if allocationID is provided, use that allocation, otherwise get the first one
		IGSNAllocation allocation = (allocationID != null)
				? (IGSNAllocation) user.getAllocationsByType(IGSNService.IGSNallocationType).stream()
				.filter(alloc -> alloc.getId().equals(UUID.fromString(allocationID))).findFirst().get()
				: (IGSNAllocation) user.getAllocationsByType(IGSNService.IGSNallocationType).get(0);

		// generate unique IGSN Value
		String igsn;
		String value;
		String prefix = allocation.getPrefix();
		String namespace = allocation.getNamespace();
		do {
			value = RandomStringUtils.randomAlphanumeric(6);
			igsn = String.format("%s/%s%s", allocation.getPrefix(), allocation.getNamespace(), value).toUpperCase();
		}
		while (identifierService.findByValueAndType(igsn, Identifier.Type.IGSN) != null);

		Map<String, Object> response = new HashMap<>();
		response.put("prefix", prefix);
		response.put("namespace", namespace);
		response.put("value", value);
		response.put("igsn", igsn);
		response.put("allocation", new ModelMapper().map(allocation, AllocationDTO.class));

		return ResponseEntity.ok().body(response);
	}

}