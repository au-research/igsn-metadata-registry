package au.edu.ardc.registry.igsn.controller;

import au.edu.ardc.registry.igsn.config.IGSNProperties;
import au.edu.ardc.registry.igsn.entity.IGSNServiceRequest;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.igsn.service.IGSNService;
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
import java.io.File;
import java.io.FileWriter;
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
    IGSNService service;

    @Autowired
    @Qualifier("standardJobLauncher")
    JobLauncher jobLauncher;

    @Autowired
    @Qualifier("ReserveIGSNJob")
    Job reserveIGSNJob;

    @PostMapping("")
    public ResponseEntity<IGSNServiceRequest> handle(
            HttpServletRequest request,
            @RequestParam UUID allocationID,
            @RequestParam(required = false, defaultValue = "User") String ownerType,
            @RequestParam(required = false) String ownerID,
            @RequestBody String IGSNList
    ) throws JobParametersInvalidException, JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {
        // todo validate request body contains 1 IGSN per line
        User user = kcService.getLoggedInUser(request);
        // todo validate ownership & allocationID & IGSNList

        if (ownerType.equals(Record.OwnerType.User.toString())) {
            ownerID = user.getId().toString();
        }
        // todo validateOwnerID if ownerType=DataCenter

        IGSNServiceRequest IGSNRequest = service.createRequest(user);
        String dataPath = IGSNRequest.getDataPath();

        // write IGSNList to input.txt
        String filePath = dataPath + "/input.txt";
        try {
            File inputIGSNFile = new File(filePath);
            if (inputIGSNFile.createNewFile()) {
                System.out.println("File created: " + inputIGSNFile.getName());
            } else {
                System.out.println("File already exists.");
            }
            FileWriter writer = new FileWriter(filePath);
            writer.write(IGSNList);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("IGSNServiceRequestID", IGSNRequest.getId().toString())
                .addString("creatorID", user.getId().toString())
                .addString("allocationID", allocationID.toString())
                .addString("ownerID", ownerID)
                .addString("ownerType", ownerType)
                .addString("filePath", filePath)
                .addString("targetPath", dataPath + "/output.txt")
                .toJobParameters();

        jobLauncher.run(reserveIGSNJob, jobParameters);

        return ResponseEntity.ok().body(IGSNRequest);
    }

}
