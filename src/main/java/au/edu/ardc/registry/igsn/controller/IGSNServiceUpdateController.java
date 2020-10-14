package au.edu.ardc.registry.igsn.controller;

import au.edu.ardc.registry.common.dto.RequestDTO;
import au.edu.ardc.registry.common.dto.mapper.RequestMapper;
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
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/services/igsn", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "IGSN Mint API")
@SecurityRequirement(name = "basic")
@SecurityRequirement(name = "oauth2")
public class IGSNServiceUpdateController {

	final IGSNRequestService igsnRequestService;

	final RequestService requestService;

	final SchemaService schemaService;

	final ValidationService validationService;

	final VersionService versionService;

	final IdentifierService identifierService;

	final RequestMapper requestMapper;

	final JobLauncher standardJobLauncher;

	final JobLauncher asyncJobLauncher;

	final Job IGSNUpdateJob;

	private final KeycloakService kcService;

	public IGSNServiceUpdateController(IGSNRequestService igsnRequestService, RequestService requestService,
									 SchemaService schemaService, ValidationService validationService, VersionService versionService,
									 IdentifierService identifierService, RequestMapper requestMapper, @Qualifier("standardJobLauncher") JobLauncher standardJobLauncher,
									 @Qualifier("asyncJobLauncher") JobLauncher asyncJobLauncher, @Qualifier("IGSNUpdateJob") Job IGSNUpdateJob,
									 KeycloakService kcService) {
		this.igsnRequestService = igsnRequestService;
		this.requestService = requestService;
		this.schemaService = schemaService;
		this.validationService = validationService;
		this.versionService = versionService;
		this.identifierService = identifierService;
		this.requestMapper = requestMapper;
		this.standardJobLauncher = standardJobLauncher;
		this.asyncJobLauncher = asyncJobLauncher;
		this.IGSNUpdateJob = IGSNUpdateJob;
		this.kcService = kcService;
	}

	/**
	 *Update IGSN Service endpoint.
	 * @param request the {@link HttpServletRequest} for this request
	 * @param payload the required {@link RequestBody} for this request
	 * @param ownerType (Optional) default is 'User'
	 * @param wait (Optional) {yes, true, 1 | no false 0}return instantly and start a
	 * background job or wait until mint is completed default is {no , false, 0}
	 * @return an IGSN response records
	 * @throws Exception when things go wrong, handled by Exception Advice
	 */
	@PostMapping("/update")
	public ResponseEntity<RequestDTO> mint(HttpServletRequest request, @RequestBody String payload,
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

}
