package au.edu.ardc.registry.oai.controller;

import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.provider.Metadata;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.oai.exception.BadVerbException;
import au.edu.ardc.registry.oai.model.*;
import au.edu.ardc.registry.oai.response.GetRecordResponse;
import au.edu.ardc.registry.oai.response.OAIIdentifyResponse;
import au.edu.ardc.registry.oai.response.OAIListMetadataFormatsResponse;
import au.edu.ardc.registry.oai.response.OAIResponse;
import au.edu.ardc.registry.common.service.RecordService;
import au.edu.ardc.registry.common.service.VersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = "/api/services/oai-pmh", produces = MediaType.APPLICATION_XML_VALUE)
public class OAIPMHService {


    @Autowired
    RecordService recordService;

    @Autowired
    VersionService versionService;

    @GetMapping(value="", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<OAIResponse> handle(
            HttpServletRequest request,
            @RequestParam(required=false) String verb,
            @RequestParam(required=false) String identifier,
            @RequestParam(required=false) String metadataPrefix
    ) {

        if (verb == null || verb.equals("")){
            throw new BadVerbException("No OAI verb supplied", "badVerb");
        }

        RequestFragment requestFragment = new RequestFragment();
        requestFragment.setValue(request.getRequestURL().toString());
        requestFragment.setVerb(verb);

        if (verb.equals("Identify")) {
            return identify(request, requestFragment);
        } else if (verb.equals("GetRecord")) {
            requestFragment.setIdentifier(identifier);
            requestFragment.setMetadataPrefix(metadataPrefix);
            return getRecord(identifier, metadataPrefix);
        } else if (verb.equals("ListRecords")) {
            throw new BadVerbException("Illegal OAI verb", "badVerb");
            //return getRecords(metadataPrefix);
        } else if (verb.equals("ListMetadataFormats")){
            throw new BadVerbException("Illegal OAI verb", "badVerb");
        } else {
            throw new BadVerbException("Illegal OAI verb", "badVerb");
        }

    }

    private ResponseEntity<OAIResponse> identify(HttpServletRequest request, RequestFragment requestFragment) {
        IdentifyFragment identify = new IdentifyFragment();
        identify.setRepositoryName("ARDC IGSN Repository");

        OAIResponse response = new OAIIdentifyResponse(identify);
        response.setRequest(requestFragment);

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_XML)
                .body(response);
    }


    private ResponseEntity<OAIResponse> getRecord(String identifier, String metadataPrefix) {
        if (identifier == null) {
            // todo badArgument Missing required argument
        }
        if (metadataPrefix == null) {
            // todo badArgument Missing required argument
        }
        // todo handle metadataPrefix not supported

        Record record = recordService.findById(identifier);
        // todo handle idDoesNotExist

        Version version = versionService.findVersionForRecord(record, metadataPrefix);
        // todo handle version not found
        String content = new String(version.getContent());

        // build GetRecordResponse
        GetRecordResponse response = new GetRecordResponse(record, content);

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_XML)
                .body(response);
    }
}
