package au.edu.ardc.registry.igsn.controller;

import au.edu.ardc.registry.common.dto.RequestDTO;
import au.edu.ardc.registry.common.dto.mapper.RequestMapper;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.model.Attribute;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.service.*;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.igsn.entity.IGSNEventType;
import au.edu.ardc.registry.igsn.service.IGSNRequestService;
import au.edu.ardc.registry.igsn.validator.PayloadValidator;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/services/igsn", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "IGSN Service")
@SecurityRequirement(name = "basic")
@SecurityRequirement(name = "oauth2")
public class IGSNServiceController {

	final RequestService requestService;

	final SchemaService schemaService;

	final ValidationService validationService;

	final VersionService versionService;

	final IdentifierService identifierService;

	final RequestMapper requestMapper;

	final JobLauncher standardJobLauncher;

	final JobLauncher asyncJobLauncher;

	final Job igsnImportJob;

	final Job IGSNUpdateJob;

	final Job transferIGSNJob;

	final Job reserveIGSNJob;

	private final IGSNRequestService igsnRequestService;

	private final KeycloakService kcService;

	public IGSNServiceController(IGSNRequestService igsnRequestService, RequestService requestService,
			SchemaService schemaService, ValidationService validationService, VersionService versionService,
			IdentifierService identifierService, RequestMapper requestMapper,
			@Qualifier("standardJobLauncher") JobLauncher standardJobLauncher,
			@Qualifier("asyncJobLauncher") JobLauncher asyncJobLauncher, @Qualifier("IGSNImportJob") Job igsnImportJob,
			@Qualifier("IGSNUpdateJob") Job igsnUpdateJob, KeycloakService kcService,
			@Qualifier("ReserveIGSNJob") Job reserveIGSNJob, @Qualifier("TransferIGSNJob") Job transferIGSNJob) {
		this.igsnRequestService = igsnRequestService;
		this.requestService = requestService;
		this.schemaService = schemaService;
		this.validationService = validationService;
		this.versionService = versionService;
		this.identifierService = identifierService;
		this.requestMapper = requestMapper;
		this.standardJobLauncher = standardJobLauncher;
		this.asyncJobLauncher = asyncJobLauncher;
		this.igsnImportJob = igsnImportJob;
		this.IGSNUpdateJob = igsnUpdateJob;
		this.kcService = kcService;
		this.reserveIGSNJob = reserveIGSNJob;
		this.transferIGSNJob = transferIGSNJob;
	}

	/**
	 * Mint IGSN Service endpoint.
	 * @param request the {@link HttpServletRequest} for this request
	 * @param payload the required {@link RequestBody} for this request
	 * @param ownerType (Optional) default is 'User'
	 * @param wait (Optional) {yes, true, 1 | no false 0}return instantly and start a
	 * background job or wait until mint is completed default is {no , false, 0}
	 * @return an IGSN response records
	 * @throws Exception when things go wrong, handled by Exception Advice
	 */
	@PostMapping("/mint")
	public ResponseEntity<RequestDTO> mint(HttpServletRequest request, @RequestBody String payload,
			@RequestParam(required = false, defaultValue = "User") String ownerType,
			@RequestParam(required = false, defaultValue = "0") boolean wait) throws Exception {
		User user = kcService.getLoggedInUser(request);

		// Validate the request
		PayloadValidator validator = new PayloadValidator(schemaService, validationService, identifierService,
				versionService);
		validator.validateMintPayload(payload, user);

		// creating the IGSN Request & write the payload to file
		Request igsnRequest = igsnRequestService.createRequest(user, IGSNEventType.MINT);
		String dataPath = requestService.getDataPathFor(igsnRequest);
		String fileExtension = Helpers.getFileExtensionForContent(payload);
		String payLoadContentPath = dataPath + File.separator + "payload" + fileExtension;
		Helpers.writeFile(payLoadContentPath, payload);

		// If All is good, then start an IGSN import and MDS mint job
		// try job execution and catch any exception
		UUID allocationID = validator.getUserAccessValidator().getAllocationID();
		// @formatter:off
		igsnRequest.setAttribute(Attribute.CREATOR_ID, user.getId().toString())
				.setAttribute(Attribute.OWNER_TYPE, ownerType)
				.setAttribute(Attribute.DATA_PATH, dataPath)
				.setAttribute(Attribute.EVENT_TYPE, IGSNEventType.MINT.toString())
				.setAttribute(Attribute.PAYLOAD_PATH, payLoadContentPath)
				.setAttribute(Attribute.CHUNKED_PAYLOAD_PATH, dataPath + File.separator + "chunks")
				.setAttribute(Attribute.ALLOCATION_ID, allocationID.toString())
				.setAttribute(Attribute.LOG_PATH, requestService.getLoggerPathFor(igsnRequest))
				.setAttribute(Attribute.REQUESTED_IDENTIFIERS_PATH, dataPath + File.separator + "igsn_list.txt");
		// @formatter:on

		JobParameters jobParameters = new JobParametersBuilder()
				.addString("IGSNServiceRequestID", igsnRequest.getId().toString()).toJobParameters();

		JobExecution jobExecution = wait ? standardJobLauncher.run(igsnImportJob, jobParameters)
				: asyncJobLauncher.run(igsnImportJob, jobParameters);

		igsnRequest.setStatus(Request.Status.ACCEPTED);
		igsnRequest.setAttribute("JobID", String.valueOf(jobExecution.getJobId()));
		igsnRequest.setAttribute("wait", String.valueOf(wait));
		igsnRequestService.save(igsnRequest);

		// store the Request into the HttpServletRequest for logging at APILogging
		request.setAttribute(String.valueOf(Request.class), igsnRequest);

		RequestDTO dto = requestMapper.getConverter().convert(igsnRequest);
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(dto);
	}

	/**
	 * Update IGSN Service endpoint.
	 * @param request the {@link HttpServletRequest} for this request
	 * @param payload the required {@link RequestBody} for this request
	 * @param ownerType (Optional) default is 'User'
	 * @param wait (Optional) {yes, true, 1 | no false 0}return instantly and start a
	 * background job or wait until mint is completed default is {no , false, 0}
	 * @return an IGSN response records
	 * @throws Exception when things go wrong, handled by Exception Advice
	 */
	@PostMapping("/update")
	public ResponseEntity<RequestDTO> update(HttpServletRequest request, @RequestBody String payload,
			@RequestParam(required = false, defaultValue = "User") String ownerType,
			@RequestParam(required = false, defaultValue = "0") boolean wait) throws Exception {
		User user = kcService.getLoggedInUser(request);

		// Validate the request
		PayloadValidator validator = new PayloadValidator(schemaService, validationService, identifierService,
				versionService);
		validator.validateUpdatePayload(payload, user);

		// creating the IGSN Request & write the payload to file
		Request igsnRequest = igsnRequestService.createRequest(user, IGSNEventType.UPDATE);
		String dataPath = requestService.getDataPathFor(igsnRequest);
		String fileExtension = Helpers.getFileExtensionForContent(payload);
		String payLoadContentPath = dataPath + File.separator + "payload" + fileExtension;
		Helpers.writeFile(payLoadContentPath, payload);

		// If All is good, then start an IGSN import and MDS mupdate job
		// try job execution and catch any exception
		UUID allocationID = validator.getUserAccessValidator().getAllocationID();
		// @formatter:off
		igsnRequest.setAttribute(Attribute.CREATOR_ID, user.getId().toString())
				.setAttribute(Attribute.OWNER_TYPE, ownerType)
				.setAttribute(Attribute.DATA_PATH, dataPath)
				.setAttribute(Attribute.EVENT_TYPE, IGSNEventType.UPDATE.toString())
				.setAttribute(Attribute.PAYLOAD_PATH, payLoadContentPath)
				.setAttribute(Attribute.CHUNKED_PAYLOAD_PATH, dataPath + File.separator + "chunks")
				.setAttribute(Attribute.ALLOCATION_ID, allocationID.toString())
				.setAttribute(Attribute.LOG_PATH, requestService.getLoggerPathFor(igsnRequest))
				.setAttribute(Attribute.REQUESTED_IDENTIFIERS_PATH, dataPath + File.separator + "igsn_list.txt");
		// @formatter:on

		JobParameters jobParameters = new JobParametersBuilder()
				.addString("IGSNServiceRequestID", igsnRequest.getId().toString()).toJobParameters();

		JobExecution jobExecution = wait ? standardJobLauncher.run(IGSNUpdateJob, jobParameters)
				: asyncJobLauncher.run(IGSNUpdateJob, jobParameters);

		igsnRequest.setStatus(Request.Status.ACCEPTED);
		igsnRequest.setAttribute("JobID", String.valueOf(jobExecution.getJobId()));
		igsnRequest.setAttribute("wait", String.valueOf(wait));
		igsnRequestService.save(igsnRequest);

		// store the Request into the HttpServletRequest for logging at APILogging
		request.setAttribute(String.valueOf(Request.class), igsnRequest);

		RequestDTO dto = requestMapper.getConverter().convert(igsnRequest);
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(dto);
	}

	@PostMapping("/reserve")
	public ResponseEntity<RequestDTO> reserve(HttpServletRequest request, @RequestParam UUID allocationID,
			@RequestParam(required = false, defaultValue = "User") String ownerType,
			@RequestParam(required = false) String ownerID, @RequestBody String IGSNList)
			throws JobParametersInvalidException, JobExecutionAlreadyRunningException, JobRestartException,
			JobInstanceAlreadyCompleteException, IOException {
		// todo validate request body contains 1 IGSN per line
		User user = kcService.getLoggedInUser(request);
		// todo validate ownership & allocationID & IGSNList

		if (ownerType.equals(Record.OwnerType.User.toString())) {
			ownerID = user.getId().toString();
		}
		// todo validateOwnerID if ownerType=DataCenter

		Request IGSNRequest = igsnRequestService.createRequest(user, IGSNEventType.RESERVE);

		// write IGSNList to input.txt
		String requestedIdentifierFilePath = IGSNRequest.getAttribute(Attribute.DATA_PATH)
				+ "/requested-identifiers.txt";
		Helpers.writeFile(requestedIdentifierFilePath, IGSNList);

		// todo imported path

		// @formatter:off
		IGSNRequest.setAttribute(Attribute.CREATOR_ID, user.getId().toString())
				.setAttribute(Attribute.OWNER_ID, ownerID)
				.setAttribute(Attribute.OWNER_TYPE, ownerType)
				.setAttribute(Attribute.DATA_PATH, requestService.getDataPathFor(IGSNRequest))
				.setAttribute(Attribute.LOG_PATH, requestService.getLoggerPathFor(IGSNRequest))
				.setAttribute(Attribute.REQUESTED_IDENTIFIERS_PATH, requestedIdentifierFilePath)
				.setAttribute(Attribute.IMPORTED_IDENTIFIERS_PATH, IGSNRequest.getAttribute(Attribute.DATA_PATH) + "/imported-identifiers.txt")
				.setAttribute(Attribute.ALLOCATION_ID, allocationID.toString());
		// @formatter:on

		JobParameters jobParameters = new JobParametersBuilder()
				.addString("IGSNServiceRequestID", IGSNRequest.getId().toString()).toJobParameters();

		standardJobLauncher.run(reserveIGSNJob, jobParameters);

		// set the IGSNServiceRequest in the request for later logging
		request.setAttribute(String.valueOf(Request.class), IGSNRequest);

		RequestDTO dto = requestMapper.convertToDTO(IGSNRequest);

		return ResponseEntity.ok().body(dto);
	}

	@PostMapping("/transfer")
	public ResponseEntity<Request> handle(HttpServletRequest request, @RequestParam UUID ownerID,
			@RequestParam String ownerType, @RequestBody String IGSNList) throws JobParametersInvalidException,
			JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException, IOException {
		User user = kcService.getLoggedInUser(request);

		Request IGSNRequest = igsnRequestService.createRequest(user, IGSNEventType.TRANSFER);

		// write IGSNList to input.txt
		String requestedIdentifierFilePath = IGSNRequest.getAttribute(Attribute.DATA_PATH)
				+ "/requested-identifiers.txt";
		Helpers.writeFile(requestedIdentifierFilePath, IGSNList);

		// todo imported path

		// @formatter:off
		IGSNRequest.setAttribute(Attribute.CREATOR_ID, user.getId().toString())
				.setAttribute(Attribute.OWNER_ID, ownerID.toString())
				.setAttribute(Attribute.OWNER_TYPE, ownerType)
				.setAttribute(Attribute.DATA_PATH, requestService.getDataPathFor(IGSNRequest))
				.setAttribute(Attribute.LOG_PATH, requestService.getLoggerPathFor(IGSNRequest))
				.setAttribute(Attribute.REQUESTED_IDENTIFIERS_PATH, requestedIdentifierFilePath)
				.setAttribute(Attribute.IMPORTED_IDENTIFIERS_PATH, IGSNRequest.getAttribute(Attribute.DATA_PATH) + "/imported-identifiers.txt");
		// @formatter:on

		JobParameters jobParameters = new JobParametersBuilder()
				.addString("IGSNServiceRequestID", IGSNRequest.getId().toString()).toJobParameters();

		standardJobLauncher.run(transferIGSNJob, jobParameters);

		// set the IGSNServiceRequest in the request for later logging
		request.setAttribute(String.valueOf(Request.class), IGSNRequest);

		return ResponseEntity.ok().body(IGSNRequest);
	}

}
