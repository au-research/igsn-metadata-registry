package au.edu.ardc.registry.igsn.controller;

import au.edu.ardc.registry.common.dto.RequestDTO;
import au.edu.ardc.registry.common.dto.mapper.RequestMapper;
import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.model.Attribute;
import au.edu.ardc.registry.common.model.Scope;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.provider.FragmentProvider;
import au.edu.ardc.registry.common.provider.Metadata;
import au.edu.ardc.registry.common.provider.MetadataProviderFactory;
import au.edu.ardc.registry.common.service.IdentifierService;
import au.edu.ardc.registry.common.service.KeycloakService;
import au.edu.ardc.registry.common.service.RequestService;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.igsn.model.IGSNAllocation;
import au.edu.ardc.registry.igsn.service.IGSNRequestService;
import au.edu.ardc.registry.igsn.service.IGSNRequestValidationService;
import au.edu.ardc.registry.igsn.service.IGSNService;
import au.edu.ardc.registry.igsn.service.ImportService;
import au.edu.ardc.registry.igsn.task.ImportIGSNTask;
import au.edu.ardc.registry.igsn.task.ReserveIGSNTask;
import au.edu.ardc.registry.igsn.task.TransferIGSNTask;
import au.edu.ardc.registry.igsn.task.UpdateIGSNTask;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/services/igsn",
		produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
@Tag(name = "IGSN Service", description = "API endpoints for IGSN related operations")
@SecurityRequirement(name = "basic")
@SecurityRequirement(name = "oauth2")
public class IGSNServiceController {

	private static final Logger logger = LoggerFactory.getLogger(IGSNServiceController.class);

	final RequestService requestService;

	final RequestMapper requestMapper;

	final IGSNService igsnService;

	private final IGSNRequestService igsnRequestService;

	private final KeycloakService kcService;

	private final IGSNRequestValidationService igsnRequestValidationService;

	private final ApplicationEventPublisher applicationEventPublisher;

	private final ImportService importService;

	private final SchemaService schemaService;

	private final IdentifierService identifierService;

	public IGSNServiceController(IGSNRequestService igsnRequestService, RequestService requestService,
			RequestMapper requestMapper, KeycloakService kcService,
			IGSNRequestValidationService igsnRequestValidationService, IGSNService igsnService,
			ApplicationEventPublisher applicationEventPublisher, ImportService importService,
			SchemaService schemaService, IdentifierService identifierService) {
		this.igsnRequestService = igsnRequestService;
		this.requestService = requestService;
		this.requestMapper = requestMapper;
		this.kcService = kcService;
		this.igsnRequestValidationService = igsnRequestValidationService;
		this.igsnService = igsnService;
		this.applicationEventPublisher = applicationEventPublisher;
		this.importService = importService;
		this.schemaService = schemaService;
		this.identifierService = identifierService;
	}

	@PostMapping("/bulk-mint")
	@Operation(summary = "Bulk mint IGSN", description = "Mint a batch of IGSN identifier with metadata")
	@ApiResponse(responseCode = "202", description = "Bulk mint request is accepted",
			content = @Content(schema = @Schema(implementation = RequestDTO.class)))
	public ResponseEntity<RequestDTO> bulkMint(HttpServletRequest httpServletRequest, @RequestBody String payload,
			@RequestParam(required = false) String ownerID, @RequestParam(required = false) String ownerType)
			throws IOException {
		User user = kcService.getLoggedInUser(httpServletRequest);

		// creating the IGSN Request & write the payload to file
		Request request = igsnRequestService.createRequest(user, IGSNService.EVENT_BULK_MINT, payload);
		if (ownerType != null) {
			request.setAttribute(Attribute.OWNER_TYPE, ownerType);
		}

		if (ownerID != null) {
			request.setAttribute(Attribute.OWNER_ID, ownerID);
		}

		// Validate the request
		igsnRequestValidationService.validate(request, user);
		request.setStatus(Request.Status.ACCEPTED);
		igsnRequestService.save(request);

		// process
		IGSNAllocation allocation = igsnService.getIGSNAllocationForContent(payload, user, Scope.CREATE);
		request.setAttribute(Attribute.ALLOCATION_ID, allocation.getId().toString());
		request.setAttribute(Attribute.CREATOR_ID, user.getId().toString());
		igsnRequestService.save(request);

		// process the request (async)
		igsnService.processMintOrUpdate(request);

		RequestDTO dto = requestMapper.getConverter().convert(request);
		return ResponseEntity.accepted().body(dto);
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
	@PostMapping("/mint")
	@Operation(summary = "Mint a new IGSN", description = "Creates a new IGSN Identifier and Metadata")
	@ApiResponse(responseCode = "201", description = "Mint request is accepted",
			content = @Content(schema = @Schema(implementation = RequestDTO.class)))
	public ResponseEntity<RequestDTO> mint(HttpServletRequest httpServletRequest, @RequestBody String payload,
			@RequestParam(required = false) String ownerID, @RequestParam(required = false) String ownerType)
			throws Exception {
		User user = kcService.getLoggedInUser(httpServletRequest);

		// creating the IGSN Request & write the payload to file
		Request request = igsnRequestService.createRequest(user, IGSNService.EVENT_MINT, payload);
		if (ownerType != null) {
			request.setAttribute(Attribute.OWNER_TYPE, ownerType);
		}

		if (ownerID != null) {
			request.setAttribute(Attribute.OWNER_ID, ownerID);
		}

		// Validate the request
		igsnRequestValidationService.validate(request, user);
		request.setStatus(Request.Status.ACCEPTED);
		igsnRequestService.save(request);

		// process (single)
		IGSNAllocation allocation = igsnService.getIGSNAllocationForContent(payload, user, Scope.CREATE);
		request.setAttribute(Attribute.ALLOCATION_ID, allocation.getId().toString());
		request.setAttribute(Attribute.CREATOR_ID, user.getId().toString());
		request.setStatus(Request.Status.PROCESSED);
		igsnRequestService.save(request);

		// run
		String payLoadContentPath = request.getAttribute(Attribute.PAYLOAD_PATH);
		FragmentProvider fragmentProvider = (FragmentProvider) MetadataProviderFactory
				.create(schemaService.getSchemaForContent(payload), Metadata.Fragment);
		String identifierValue = fragmentProvider.get(payload, 0);
		ImportIGSNTask task = new ImportIGSNTask(identifierValue, new File(payLoadContentPath), request, importService,
				applicationEventPublisher, igsnRequestService);
		task.run();

		// finish request
		igsnService.finalizeRequest(request);

		RequestDTO dto = requestMapper.getConverter().convert(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(dto);
	}

	/**
	 * Update IGSN Service endpoint.
	 * @param httpServletRequest the {@link HttpServletRequest} for this request
	 * @param payload the required {@link RequestBody} for this request
	 * @return an IGSN response records
	 * @throws Exception when things go wrong, handled by Exception Advice
	 */
	@PostMapping("/update")
	@ApiResponse(responseCode = "201", description = "Update request is accepted",
			content = @Content(schema = @Schema(implementation = RequestDTO.class)))
	@Operation(summary = "Updates an existing IGSN metadata", description = "Updates an existing IGSN Metadata")
	public ResponseEntity<RequestDTO> update(HttpServletRequest httpServletRequest, @RequestBody String payload)
			throws Exception {
		User user = kcService.getLoggedInUser(httpServletRequest);

		// creating the IGSN Request & write the payload to file
		Request request = igsnRequestService.createRequest(user, IGSNService.EVENT_UPDATE, payload);

		// Validate the request
		igsnRequestValidationService.validate(request, user);
		request.setStatus(Request.Status.ACCEPTED);
		igsnRequestService.save(request);

		// process
		IGSNAllocation allocation = igsnService.getIGSNAllocationForContent(payload, user, Scope.UPDATE);
		request.setAttribute(Attribute.ALLOCATION_ID, allocation.getId().toString());
		request.setAttribute(Attribute.CREATOR_ID, user.getId().toString());
		request.setStatus(Request.Status.PROCESSED);
		igsnRequestService.save(request);

		// run
		String payLoadContentPath = request.getAttribute(Attribute.PAYLOAD_PATH);
		FragmentProvider fragmentProvider = (FragmentProvider) MetadataProviderFactory
				.create(schemaService.getSchemaForContent(payload), Metadata.Fragment);
		String identifierValue = fragmentProvider.get(payload, 0);
		UpdateIGSNTask task = new UpdateIGSNTask(identifierValue, new File(payLoadContentPath), request, importService,
				applicationEventPublisher,igsnRequestService);
		task.run();

		// finish
		igsnService.finalizeRequest(request);

		RequestDTO dto = requestMapper.getConverter().convert(request);
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(dto);
	}

	@PostMapping("/bulk-update")
	@Operation(summary = "Bulk update IGSN", description = "Update a batch of IGSN identifier with metadata")
	@ApiResponse(responseCode = "202", description = "Bulk update request is accepted",
			content = @Content(schema = @Schema(implementation = RequestDTO.class)))
	public ResponseEntity<RequestDTO> bulkUpdate(HttpServletRequest httpServletRequest, @RequestBody String payload)
			throws Exception {
		User user = kcService.getLoggedInUser(httpServletRequest);

		// creating the IGSN Request & write the payload to file
		Request request = igsnRequestService.createRequest(user, IGSNService.EVENT_BULK_UPDATE, payload);

		// Validate the request
		igsnRequestValidationService.validate(request, user);
		request.setStatus(Request.Status.ACCEPTED);
		igsnRequestService.save(request);

		// process
		IGSNAllocation allocation = igsnService.getIGSNAllocationForContent(payload, user, Scope.UPDATE);
		request.setAttribute(Attribute.ALLOCATION_ID, allocation.getId().toString());
		request.setAttribute(Attribute.CREATOR_ID, user.getId().toString());
		request.setStatus(Request.Status.PROCESSED);
		igsnRequestService.save(request);

		igsnService.processMintOrUpdate(request);

		RequestDTO dto = requestMapper.getConverter().convert(request);
		return ResponseEntity.accepted().body(dto);
	}

	@PostMapping("/reserve")
	@Operation(summary = "Reserve a set of IGSN",
			description = "Reserved IGSN would not require metadata and will not be resolvable")
	@ApiResponse(responseCode = "200", description = "Reserve request has finished successfully",
			content = @Content(schema = @Schema(implementation = RequestDTO.class)))
	public ResponseEntity<RequestDTO> reserve(HttpServletRequest httpServletRequest, @NotNull @RequestParam UUID allocationID,
											  @RequestParam(required = false, defaultValue = "User") String ownerType,
											  @RequestParam(required = false) String ownerID, @RequestBody String payload) throws IOException {
		User user = kcService.getLoggedInUser(httpServletRequest);
		Request request = igsnRequestService.createRequest(user, IGSNService.EVENT_RESERVE, payload);

		// todo validate the request
		igsnRequestValidationService.validate(request, user);
		request.setStatus(Request.Status.ACCEPTED);
		igsnRequestService.save(request);

		// process
		// todo obtain allocationID from payload instead
		request.setAttribute(Attribute.ALLOCATION_ID, allocationID.toString());
		request.setStatus(Request.Status.PROCESSED);
		igsnRequestService.save(request);

		// run
		// todo handle this better (validate each line)
		String[] lines = payload.split("\\r?\\n");
		for (String identifierValue : lines) {
			ReserveIGSNTask task = new ReserveIGSNTask(identifierValue, request, importService);
			task.run();
		}

		igsnService.finalizeRequest(request);

		RequestDTO dto = requestMapper.getConverter().convert(request);
		return ResponseEntity.ok().body(dto);
	}

	@PostMapping("/transfer")
	@Operation(summary = "Transfer a set of IGSNs to another User/DataCenter",
			description = "Transfer the ownership of a list of IGSN Identifier to another User or DataCenter")
	@ApiResponse(responseCode = "200", description = "Transfer request has completed successfully",
			content = @Content(schema = @Schema(implementation = RequestDTO.class)))
	public ResponseEntity<RequestDTO> handle(HttpServletRequest httpServletRequest, @NotNull @RequestParam UUID ownerID,
											 @RequestParam String ownerType, @RequestBody String payload) throws IOException {
		User user = kcService.getLoggedInUser(httpServletRequest);

		Request request = igsnRequestService.createRequest(user, IGSNService.EVENT_TRANSFER, payload);

		// todo validate
		request.setStatus(Request.Status.ACCEPTED);
		igsnRequestService.save(request);

		// process
		request.setAttribute(Attribute.OWNER_ID, ownerID.toString());
		request.setAttribute(Attribute.OWNER_TYPE, ownerType);
		request.setStatus(Request.Status.PROCESSED);
		igsnRequestService.save(request);

		// run
		// todo handle this better (validate each line)
		String[] lines = payload.split("\\r?\\n");
		for (String identifierValue : lines) {
			TransferIGSNTask task = new TransferIGSNTask(identifierValue, request, importService);
			task.run();
		}

		igsnService.finalizeRequest(request);

		RequestDTO dto = requestMapper.getConverter().convert(request);
		return ResponseEntity.ok().body(dto);
	}

	@GetMapping("/generate-igsn")
	public ResponseEntity<?> generateIGSN(HttpServletRequest httpServletRequest) {
		User user = kcService.getLoggedInUser(httpServletRequest);

		// currently only support generation of IGSN belongs to the first Allocation
		IGSNAllocation allocation = (IGSNAllocation) user.getAllocationsByType(IGSNService.IGSNallocationType).get(0);

		// generate unique IGSN Value
		String igsnValue;
		do {
			igsnValue = String.format("%s/%s%s", allocation.getPrefix(), allocation.getNamespace(),
					RandomStringUtils.randomAlphanumeric(6)).toUpperCase();
		} while (identifierService.findByValueAndType(igsnValue, Identifier.Type.IGSN) != null);

		return ResponseEntity.ok().body(igsnValue);
	}

}
