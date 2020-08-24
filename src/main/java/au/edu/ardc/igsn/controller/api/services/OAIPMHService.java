package au.edu.ardc.igsn.controller.api.services;

import au.edu.ardc.igsn.exception.oai.BadVerbException;
import au.edu.ardc.igsn.oai.model.Identify;
import au.edu.ardc.igsn.oai.response.OAIIdentifyResponse;
import au.edu.ardc.igsn.oai.response.OAIResponse;
import au.edu.ardc.igsn.repository.VersionRepository;
import au.edu.ardc.igsn.service.VersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping(value = "/api/services/oai-pmh", produces = MediaType.APPLICATION_XML_VALUE)
public class OAIPMHService {

    @GetMapping(value="", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<OAIResponse> handle(
            @RequestParam String verb
    ) {
        if (verb.equals("Identify")) {
            Identify identify = new Identify();
            identify.setRepositoryName("ARDC IGSN Repository");
            OAIResponse response = new OAIIdentifyResponse(identify);

            return ResponseEntity.status(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_XML)
                    .body(response);
        }

        throw new BadVerbException();
    }
}
