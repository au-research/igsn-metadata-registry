package au.edu.ardc.registry.igsn.controller;

import au.edu.ardc.registry.common.dto.RequestDTO;
import au.edu.ardc.registry.common.dto.mapper.RequestMapper;
import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.model.Attribute;
import au.edu.ardc.registry.common.model.Scope;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.service.KeycloakService;
import au.edu.ardc.registry.common.service.RequestService;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.igsn.entity.IGSNEventType;
import au.edu.ardc.registry.igsn.model.IGSNAllocation;
import au.edu.ardc.registry.igsn.model.IGSNTask;
import au.edu.ardc.registry.igsn.service.IGSNRequestService;
import au.edu.ardc.registry.igsn.service.IGSNRequestValidationService;
import au.edu.ardc.registry.igsn.service.IGSNService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	public IGSNServiceController(IGSNRequestService igsnRequestService, RequestService requestService,
			RequestMapper requestMapper, KeycloakService kcService,
			IGSNRequestValidationService igsnRequestValidationService, IGSNService igsnService) {
		this.igsnRequestService = igsnRequestService;
		this.requestService = requestService;
		this.requestMapper = requestMapper;
		this.kcService = kcService;
		this.igsnRequestValidationService = igsnRequestValidationService;
		this.igsnService = igsnService;
	}

	// @GetMapping("/test")
	// public ResponseEntity<?> test() throws InterruptedException {
	// logger.info("Start test1");
	// igsnService.populateJobs();
	// logger.info("After test1");
	//
	// return ResponseEntity.ok("done");
	// }
	//
	// @GetMapping("/test2")
	// public ResponseEntity<?> test2() {
	// return ResponseEntity.ok(igsnService.getImportQueue());
	// }
	//
	// @GetMapping("/test3")
	// public ResponseEntity<?> test3() {
	// return ResponseEntity.ok(igsnService.getIdentifierLock());
	// }

	@PostMapping("/bulk-mint")
	public ResponseEntity<RequestDTO> bulkMint(HttpServletRequest httpServletRequest, @RequestBody String payload)
			throws IOException {
		User user = kcService.getLoggedInUser(httpServletRequest);

		// creating the IGSN Request & write the payload to file
		Request request = igsnRequestService.createRequest(user, IGSNService.EVENT_BULK_MINT, payload);

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
		return ResponseEntity.ok(dto);
	}

	/**
	 * Mint IGSN Service endpoint.
	 * @param httpServletRequest the {@link HttpServletRequest} for this request
	 * @param payload the required {@link RequestBody} for this request background job or
	 * wait until mint is completed default is {no , false, 0}
	 * @return an IGSN response records
	 * @throws Exception when things go wrong, handled by Exception Advice
	 */
	@PostMapping("/mint")
	@Operation(summary = "Mint a new IGSN", description = "Creates a new IGSN Identifier and Metadata")
	@ApiResponse(responseCode = "201", description = "Mint request is accepted",
			content = @Content(schema = @Schema(implementation = RequestDTO.class)))
	public ResponseEntity<RequestDTO> mint(HttpServletRequest httpServletRequest, @RequestBody String payload)
			throws Exception {
		User user = kcService.getLoggedInUser(httpServletRequest);

		// creating the IGSN Request & write the payload to file
		Request request = igsnRequestService.createRequest(user, IGSNService.EVENT_MINT, payload);

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
		IGSNTask importTask = new IGSNTask(IGSNTask.TASK_IMPORT, new File(payLoadContentPath), request.getId());
		igsnService.executeTask(importTask);
		request.setStatus(Request.Status.COMPLETED);
		igsnRequestService.save(request);

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
		IGSNTask importTask = new IGSNTask(IGSNTask.TASK_UPDATE, new File(payLoadContentPath), request.getId());
		igsnService.executeTask(importTask);
		request.setStatus(Request.Status.COMPLETED);
		igsnRequestService.save(request);

		RequestDTO dto = requestMapper.getConverter().convert(request);
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(dto);
	}

	@PostMapping("/bulk-update")
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
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(dto);
	}

	@PostMapping("/reserve")
	@Operation(summary = "Reserve a set of IGSN",
			description = "Reserved IGSN would not require metadata and will not be resolvable")
	@ApiResponse(responseCode = "200", description = "Reserve request has finished successfully",
			content = @Content(schema = @Schema(implementation = RequestDTO.class)))
	public ResponseEntity<?> reserve(HttpServletRequest httpServletRequest, @RequestParam UUID allocationID,
			@RequestParam(required = false, defaultValue = "User") String ownerType,
			@RequestParam(required = false) String ownerID, @RequestBody String IGSNList) {
		User user = kcService.getLoggedInUser(httpServletRequest);
		// todo
		return ResponseEntity.badRequest().body("Not Implemented");
	}

	@PostMapping("/transfer")
	@Operation(summary = "Transfer a set of IGSNs to another User/DataCenter",
			description = "Transfer the ownership of a list of IGSN Identifier to another User or DataCenter")
	@ApiResponse(responseCode = "200", description = "Transfer request has completed successfully",
			content = @Content(schema = @Schema(implementation = RequestDTO.class)))
	public ResponseEntity<?> handle(HttpServletRequest httpServletRequest, @RequestParam UUID ownerID,
			@RequestParam String ownerType, @RequestBody String IGSNList) {
		User user = kcService.getLoggedInUser(httpServletRequest);
		// todo
		return ResponseEntity.badRequest().body("Not Implemented");
	}

}