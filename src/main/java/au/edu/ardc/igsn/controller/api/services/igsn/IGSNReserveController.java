package au.edu.ardc.igsn.controller.api.services.igsn;

import au.edu.ardc.igsn.config.IGSNProperties;
import au.edu.ardc.igsn.entity.IGSNServiceRequest;
import au.edu.ardc.igsn.model.User;
import au.edu.ardc.igsn.service.IGSNService;
import au.edu.ardc.igsn.service.KeycloakService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

@Controller
@RequestMapping("/api/services/igsn/reserve")
public class IGSNReserveController {

    @Autowired
    KeycloakService kcService;

    @Autowired
    IGSNProperties IGSNProperties;

    @Autowired
    IGSNService service;

    @PostMapping("")
    public ResponseEntity<IGSNServiceRequest> handle(
            HttpServletRequest request,
            @RequestParam UUID allocationID,
            @RequestParam(required = false, defaultValue = "User") String ownerType,
            @RequestParam(required = false) String ownerID,
            @RequestBody String IGSNList
    ) {
        // todo validate request body contains IGSN 1 by 1, limit 500?
        // todo validateOwnerID if ownerType=DataCenter
        User user = kcService.getLoggedInUser(request);
        // todo validate user & allocationID & IGSNList

        IGSNServiceRequest IGSNRequest = service.createRequest(user);
//        IGSNRequest.setProperty("dataPath", IGSNProperties.getDataPath());

        // persist and get the id
        // store the IGSNList in a .txt file

        // dispatch reserveigsnjob with the IGSNServiceRequest id
        return ResponseEntity.ok().body(IGSNRequest);
    }

}
