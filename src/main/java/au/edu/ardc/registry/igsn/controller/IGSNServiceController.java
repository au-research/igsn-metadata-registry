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
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "the Bulk XML payload"),
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
	@PostMapping(value = "/mint", consumes = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE,
			MediaType.TEXT_PLAIN_VALUE })
	@Operation(summary = "Mint a new IGSN", description = "Creates a new IGSN Identifier and Metadata",
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "the XML payload"),
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
	@PostMapping(value = "/update", consumes = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE,
			MediaType.TEXT_PLAIN_VALUE })
	@Operation(summary = "Update IGSN", description = "Updates an existing IGSN metadata",
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
					description = "the updated XML payload"),
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

	@PostMapping(value = "/bulk-update", consumes = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE,
			MediaType.TEXT_PLAIN_VALUE })
	@Operation(summary = "Bulk Update IGSN", description = "Updates many IGSNs metadata in a single payload",
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
					description = "the updated XML payload"),
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
					description = "the newline separated IGSN list, 1 per line"),
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
			@RequestParam(required = false, defaultValue = "igsn_list") String schemaId,
			@RequestParam(required = false, defaultValue = "User") String ownerType,
			@RequestParam(required = false) String ownerID, @RequestBody String payload) throws IOException {
		User user = keycloakService.getLoggedInUser(httpServletRequest);
		Request request = igsnRequestService.createRequest(user, IGSNService.EVENT_RESERVE, payload);
		request.setAttribute(Attribute.SCHEMA_ID, schemaId);
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
			description = "Transfer the ownership of a list of IGSN Identifier to another Owner",
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
					description = "the newline separated IGSN list, 1 per line"),
			parameters = {
					@Parameter(name = "schemaID", description = "the schema of the payload",
							schema = @Schema(description = "Schema ID", type = "string",
									allowableValues = { "igsn_list" }, defaultValue = "igsn_list")),
					@Parameter(name = "ownerID", required = true, description = "The UUID of the intended Owner",
							schema = @Schema(implementation = UUID.class)),
					@Parameter(name = "ownerType", description = "The Type of the Owner", required = true,
							schema = @Schema(description = "Owner Type", type = "string", defaultValue = "DataCenter",
									allowableValues = { "User", "DataCenter" })) },
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
