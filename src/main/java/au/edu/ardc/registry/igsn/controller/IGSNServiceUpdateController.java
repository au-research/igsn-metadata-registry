package au.edu.ardc.registry.igsn.controller;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import au.edu.ardc.registry.common.model.Attribute;
import au.edu.ardc.registry.common.service.*;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.exception.*;
import au.edu.ardc.registry.igsn.entity.IGSNEventType;
import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.igsn.service.IGSNRequestService;
import au.edu.ardc.registry.igsn.validator.ContentValidator;
import au.edu.ardc.registry.igsn.validator.PayloadValidator;
import au.edu.ardc.registry.igsn.validator.UserAccessValidator;
import au.edu.ardc.registry.igsn.validator.VersionContentValidator;
import org.apache.http.HttpStatus;
import org.slf4j.MDC;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping(value = "/api/services/igsn", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Version Resource API")
@SecurityRequirement(name = "basic")
@SecurityRequirement(name = "oauth2")
public class IGSNServiceUpdateController {

	@Autowired
	private KeycloakService kcService;

	@Autowired
	IGSNRequestService igsnService;

	@Autowired
	SchemaService schemaService;

	@Autowired
	ValidationService validationService;

	@Autowired
	IdentifierService identifierService;

	@Autowired
	RecordService recordService;

	@Autowired
	VersionService versionService;

	@Autowired
	@Qualifier("standardJobLauncher")
	JobLauncher standardJobLauncher;

	@Autowired
	@Qualifier("asyncJobLauncher")
	JobLauncher asyncJobLauncher;

	@Autowired
	@Qualifier("IGSNUpdateJob")
	Job igsnUpdateJob;

	/**
	 * @param request the entire http request object
	 * @param ownerType (Optional) default is 'User'
	 * @param wait (Optional) {yes, true, 1 | no false 0}return instantly and start a
	 * background job or wait until update is completed default is {no , false, 0}
	 * @return an IGSN response
	 * @throws IOException if content an not be accessed or saved
	 * @throws ContentNotSupportedException if content is not supported as per schema.json
	 * @throws XMLValidationException if content is XML but it's invalid
	 * @throws JSONValidationException if content is JSON but it's invalid
	 * @throws ForbiddenOperationException if user has no access rights to the given
	 * records
	 */
	@PostMapping("/update")
	@Operation(summary = "Update existing IGSN record(s)", description = "Update IGSN record(s) to the registry")
	@ApiResponse(responseCode = "202", description = "IGSN Record(s) accepted",
			content = @Content(schema = @Schema(implementation = Record.class)))
	@ApiResponse(responseCode = "403", description = "Operation is forbidden",
			content = @Content(schema = @Schema(implementation = APIExceptionResponse.class)))
	public ResponseEntity<Request> update(HttpServletRequest request,
                                          @RequestParam(required = false, defaultValue = "User") String ownerType,
                                          @RequestParam(required = false, defaultValue = "0") boolean wait)
			throws IOException, ContentNotSupportedException, XMLValidationException, JSONValidationException,
			ForbiddenOperationException, APIException {
		User user = kcService.getLoggedInUser(request);
		Request igsnRequest = igsnService.createRequest(user, IGSNEventType.UPDATE);
		String dataPath = igsnRequest.getAttribute(Attribute.DATA_PATH);
		String payLoadContentPath = "";
		// validates XML or JSON content against its schema
		ContentValidator contentValidator = new ContentValidator(schemaService);
		// tests for the user's access to the records with the given IGSN Identifiers
		UserAccessValidator userAccessValidator = new UserAccessValidator(identifierService, validationService,
				schemaService);
		// compares existing versions for the given records
		// rejects records if current version iun the registry already contains the given
		// content
		VersionContentValidator versionContentValidator = new VersionContentValidator(identifierService, versionService,
				schemaService);

		PayloadValidator validator = new PayloadValidator(contentValidator, versionContentValidator,
				userAccessValidator);

		String payload = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
		String fileExtension = Helpers.getFileExtensionForContent(payload);
		payLoadContentPath = dataPath + File.separator + "payload" + fileExtension;
		Helpers.writeFile(payLoadContentPath, payload);
		// throws validation exception is anything is wrong with the payload for the given
		// user
		// to update the registry content
		validator.validateUpdatePayload(payload, user);
		// If All is good, then start an IGSN import and MDS update job
		// try job execution and catch any exception
		UUID allocationID = userAccessValidator.getAllocationID();

		try {
			JobParameters jobParameters = new JobParametersBuilder()
					.addString("IGSNServiceRequestID", igsnRequest.getId().toString())
					.addString("creatorID", user.getId().toString()).addString("payLoadContentFile", payLoadContentPath)
					.addString("allocationID", allocationID.toString()).addString("ownerType", ownerType)
					.addString("chunkContentsDir", dataPath + File.separator + "chunks")
					.addString("filePath", dataPath + File.separator + "igsn_list.txt").addString("dataPath", dataPath)
					.toJobParameters();
			if (wait) {
				standardJobLauncher.run(igsnUpdateJob, jobParameters);
			}
			else {
				asyncJobLauncher.run(igsnUpdateJob, jobParameters);
			}
		}
		catch (JobParametersInvalidException | JobExecutionAlreadyRunningException | JobRestartException
				| JobInstanceAlreadyCompleteException e) {
			throw new APIException(e.getMessage());
		}
		igsnRequest.setStatus(Request.Status.ACCEPTED);
		request.setAttribute(String.valueOf(Request.class), igsnRequest);
		MDC.put("event.action", "update-request");

		request.setAttribute(String.valueOf(Request.class), igsnRequest);
		return ResponseEntity.status(HttpStatus.SC_ACCEPTED).body(igsnRequest);
	}

}
