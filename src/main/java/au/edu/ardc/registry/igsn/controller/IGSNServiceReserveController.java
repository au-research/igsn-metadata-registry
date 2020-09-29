package au.edu.ardc.registry.igsn.controller;

import au.edu.ardc.registry.common.dto.RequestDTO;
import au.edu.ardc.registry.common.dto.mapper.RequestMapper;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.model.Attribute;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.service.KeycloakService;
import au.edu.ardc.registry.common.service.RequestService;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.igsn.entity.IGSNEventType;
import au.edu.ardc.registry.igsn.service.IGSNRequestService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;

@Controller
@RequestMapping("/api/services/igsn/reserve")
public class IGSNServiceReserveController {

	@Autowired
	KeycloakService kcService;

	@Autowired
	IGSNRequestService service;

	@Autowired
	RequestService requestService;

	@Autowired
	RequestMapper requestMapper;

	@Autowired
	@Qualifier("standardJobLauncher")
	JobLauncher jobLauncher;

	@Autowired
	@Qualifier("ReserveIGSNJob")
	Job reserveIGSNJob;

	@PostMapping("")
	public ResponseEntity<RequestDTO> handle(HttpServletRequest request, @RequestParam UUID allocationID,
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

		Request IGSNRequest = service.createRequest(user, IGSNEventType.RESERVE);

		// write IGSNList to input.txt
		String requestedIdentifierFilePath = IGSNRequest.getAttribute(Attribute.DATA_PATH)
				+ "/requested-identifiers.txt";
		Helpers.writeFile(requestedIdentifierFilePath, IGSNList);

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

		jobLauncher.run(reserveIGSNJob, jobParameters);

		// set the IGSNServiceRequest in the request for later logging
		request.setAttribute(String.valueOf(Request.class), IGSNRequest);

		RequestDTO dto = requestMapper.convertToDTO(IGSNRequest);

		return ResponseEntity.ok().body(dto);
	}

}
