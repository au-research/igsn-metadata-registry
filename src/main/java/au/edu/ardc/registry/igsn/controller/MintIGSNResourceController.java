package au.edu.ardc.registry.igsn.controller;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import au.edu.ardc.registry.common.model.Allocation;
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
public class MintIGSNResourceController {

	@Autowired
	private KeycloakService kcService;

	@Autowired
	IGSNService igsnService;

	@Autowired
	SchemaService schemaService;

	@Autowired
	ValidationService validationService;

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
	@Qualifier("IGSNImportJob")
	Job igsnImportJob;

	/**
	 * @param request the entire http request object
	 * @param ownerType (Optional default is User)
	 * @return an IGSN response
	 * @throws IOException if content an not be accessed or saved
	 * @throws ContentNotSupportedException if content is not supported as per schema.json
	 * @throws XMLValidationException if content is XML but it's invalid
	 * @throws JSONValidationException if content is JSON but it's invalid
	 * @throws ForbiddenOperationException if user has no access rights to the given
	 * records
	 */
	@PostMapping("/mint")
	@Operation(summary = "Creates new IGSN record(s)", description = "Add new IGSN record(s) to the registry")
	@ApiResponse(responseCode = "202", description = "IGSN Record(s) accepted",
			content = @Content(schema = @Schema(implementation = Record.class)))
	@ApiResponse(responseCode = "403", description = "Operation is forbidden",
			content = @Content(schema = @Schema(implementation = APIExceptionResponse.class)))
	public ResponseEntity<IGSNServiceRequest> mint(HttpServletRequest request,
			@RequestParam(required = false, defaultValue = "User") String ownerType)
			throws IOException, ContentNotSupportedException, XMLValidationException, JSONValidationException,
			ForbiddenOperationException, APIException {
		User user = kcService.getLoggedInUser(request);
		IGSNServiceRequest igsnRequest = igsnService.createRequest(user, IGSNEventType.MINT);
		String dataPath = igsnRequest.getDataPath();

		String payLoadContentPath = "";
		ContentValidator contentValidator = new ContentValidator(schemaService);
		UserAccessValidator userAccessValidator = new UserAccessValidator(identifierService, validationService,
				schemaService);
		// to validate records to MINT we don't need versionContentValidator
		// since no existing version should exist
		PayloadValidator validator = new PayloadValidator(contentValidator, null, userAccessValidator);

		String payload = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
		String fileExtension = Helpers.getFileExtensionForContent(payload);
		payLoadContentPath = dataPath + File.separator + "payload" + fileExtension;
		Helpers.writeFile(payLoadContentPath, payload);
		validator.validateMintPayload(payload, user);

		// If All is good, then start an IGSN import and MDS mint job
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
			jobLauncher.run(igsnImportJob, jobParameters);
		}
		catch (JobParametersInvalidException | JobExecutionAlreadyRunningException | JobRestartException
				| JobInstanceAlreadyCompleteException e) {
			throw new APIException(e.getMessage());
		}
		igsnRequest.setStatus(IGSNServiceRequest.Status.ACCEPTED);
		MDC.put("event.action", "mint-request");
		request.setAttribute(String.valueOf(IGSNServiceRequest.class), igsnRequest);
		return ResponseEntity.status(HttpStatus.SC_ACCEPTED).body(igsnRequest);
	}

}
