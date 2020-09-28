package au.edu.ardc.registry.igsn.controller;

import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.igsn.config.IGSNProperties;
import au.edu.ardc.registry.igsn.entity.IGSNEventType;
import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.igsn.service.IGSNRequestService;
import au.edu.ardc.registry.common.service.KeycloakService;
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
	IGSNProperties IGSNProperties;

	@Autowired
	IGSNRequestService service;

	@Autowired
	@Qualifier("standardJobLauncher")
	JobLauncher jobLauncher;

	@Autowired
	@Qualifier("ReserveIGSNJob")
	Job reserveIGSNJob;

	@PostMapping("")
	public ResponseEntity<Request> handle(HttpServletRequest request, @RequestParam UUID allocationID,
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
		String dataPath = IGSNRequest.getDataPath();

		// write IGSNList to input.txt
		String filePath = dataPath + "/input.txt";
		Helpers.writeFile(filePath, IGSNList);

		JobParameters jobParameters = new JobParametersBuilder()
				.addString("IGSNServiceRequestID", IGSNRequest.getId().toString())
				.addString("creatorID", user.getId().toString()).addString("allocationID", allocationID.toString())
				.addString("ownerID", ownerID).addString("ownerType", ownerType).addString("filePath", filePath)
				.addString("targetPath", dataPath + "/output.txt").toJobParameters();

		jobLauncher.run(reserveIGSNJob, jobParameters);

		// set the IGSNServiceRequest in the request for later logging
		request.setAttribute(String.valueOf(Request.class), IGSNRequest);

		return ResponseEntity.ok().body(IGSNRequest);
	}

}
