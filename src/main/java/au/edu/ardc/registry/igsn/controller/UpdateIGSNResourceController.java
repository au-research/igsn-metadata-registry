package au.edu.ardc.registry.igsn.controller;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import au.edu.ardc.registry.common.repository.IdentifierRepository;
import au.edu.ardc.registry.common.service.*;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.exception.*;
import au.edu.ardc.registry.igsn.entity.IGSNEventType;
import au.edu.ardc.registry.igsn.entity.IGSNServiceRequest;
import au.edu.ardc.registry.igsn.service.IGSNService;
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
@RequestMapping(value = "/api/resources/igsn", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Version Resource API")
@SecurityRequirement(name = "basic")
@SecurityRequirement(name = "oauth2")
public class UpdateIGSNResourceController {

	@Autowired
	private KeycloakService kcService;

	@Autowired
	IGSNService igsnService;

	@Autowired
	SchemaService schemaService;

	@Autowired
	ValidationService validationService;

	@Autowired
	IdentifierRepository identifierRepository;

	@Autowired
	RecordService recordService;

	@Autowired
	VersionService versionService;

	@Autowired
	IdentifierService identifierService;

	@Autowired
	@Qualifier("standardJobLauncher")
	JobLauncher jobLauncher;

	@Autowired
	@Qualifier("IGSNUpdateJob")
	Job igsnUpdateJob;

	@PostMapping("/update")
	@Operation(summary = "Update existing IGSN record(s)", description = "Update IGSN record(s) to the registry")
	@ApiResponse(responseCode = "202", description = "IGSN Record(s) accepted",
			content = @Content(schema = @Schema(implementation = Record.class)))
	@ApiResponse(responseCode = "403", description = "Operation is forbidden",
			content = @Content(schema = @Schema(implementation = APIExceptionResponse.class)))
	public ResponseEntity<IGSNServiceRequest> update(HttpServletRequest request,
			@RequestParam(required = false, defaultValue = "User") String ownerType)
			throws IOException, ContentNotSupportedException, XMLValidationException, JSONValidationException,
			ForbiddenOperationException, APIException {
		User user = kcService.getLoggedInUser(request);
		IGSNServiceRequest IGSNRequest = igsnService.createRequest(user, IGSNEventType.UPDATE);
		String dataPath = IGSNRequest.getDataPath();
		boolean isValidPayload = false;
		String payLoadContentPath = "";
		ContentValidator contentValidator = new ContentValidator(schemaService);
		UserAccessValidator userAccessValidator = new UserAccessValidator(identifierRepository, validationService,
				schemaService);
		VersionContentValidator versionContentValidator = new VersionContentValidator(recordService, versionService,
				identifierService, schemaService);

		PayloadValidator validator = new PayloadValidator(contentValidator, versionContentValidator,
				userAccessValidator);
		// try {
		String payload = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
		String fileExtension = Helpers.getFileExtensionForContent(payload);
		payLoadContentPath = dataPath + File.separator + "payload" + fileExtension;
		Helpers.writeFile(payLoadContentPath, payload);
		isValidPayload = validator.isValidUpdatePayload(payload, user);
		// }catch (Exception e){
		// throw new ContentNotSupportedException("Content Not Supported: " +
		// e.getMessage());
		// }

		if (isValidPayload) {
			// If All is good, then start an IGSN import and MDS update job
			// try job execution and catch any exception

			try {
				JobParameters jobParameters = new JobParametersBuilder()
						.addString("IGSNServiceRequestID", IGSNRequest.getId().toString())
						.addString("creatorID", user.getId().toString())
						.addString("payLoadContentFile", payLoadContentPath).addString("ownerType", ownerType)
						.addString("chunkContentsDir", dataPath + File.separator + "chunks")
						.addString("filePath", dataPath + File.separator + "igsn_list.txt")
						.addString("dataPath", dataPath).toJobParameters();

				jobLauncher.run(igsnUpdateJob, jobParameters);
			}
			catch (JobParametersInvalidException | JobExecutionAlreadyRunningException | JobRestartException
					| JobInstanceAlreadyCompleteException e) {
				throw new APIException(e.getMessage());
			}
			request.setAttribute(String.valueOf(IGSNServiceRequest.class), IGSNRequest);
			MDC.put("event.action", "update-request");
		}
		return ResponseEntity.status(HttpStatus.SC_ACCEPTED).body(IGSNRequest);
	}

}
