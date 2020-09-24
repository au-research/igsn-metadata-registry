package au.edu.ardc.registry.oai.controller;

import au.edu.ardc.registry.common.config.ApplicationProperties;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.oai.exception.BadVerbException;
import au.edu.ardc.registry.oai.model.*;
import au.edu.ardc.registry.oai.response.*;
import au.edu.ardc.registry.common.service.RecordService;
import au.edu.ardc.registry.common.service.VersionService;
import au.edu.ardc.registry.oai.service.OAIPMHService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Controller
@RequestMapping(value = "/api/services/oai-pmh", produces = MediaType.APPLICATION_XML_VALUE)
public class OAIPMHController {

	@Autowired
	SchemaService schemaService;

	@Autowired
	RecordService recordService;

	@Autowired
	VersionService versionService;

	@Autowired
	ApplicationProperties applicationProperties;

	@Autowired
	OAIPMHService oaipmhService;

	@GetMapping(value = "", produces = MediaType.APPLICATION_XML_VALUE)
	public ResponseEntity<OAIResponse> handle(HttpServletRequest request,
			@RequestParam(required = false, defaultValue = "") String verb,
			@RequestParam(required = false) String identifier, @RequestParam(required = false) String metadataPrefix,
			@RequestParam(required = false) String resumptionToken) throws IOException {

		if (!oaipmhService.isValidVerb(verb))
			throw new BadVerbException();

		RequestFragment requestFragment = new RequestFragment();
		requestFragment.setValue(request.getRequestURL().toString());
		requestFragment.setVerb(verb);

		OAIResponse response = new OAIResponse();
		switch (OAIPMHService.Verb.valueOf(verb.toUpperCase())) {
		case IDENTIFY:
			response = oaipmhService.identify();
			break;
		case GETRECORD:
			response = oaipmhService.getRecord(metadataPrefix, identifier);
			requestFragment.setIdentifier(identifier);
			requestFragment.setMetadataPrefix(metadataPrefix);
			break;
		case LISTRECORDS:
			response = oaipmhService.listRecords(metadataPrefix, resumptionToken);
			requestFragment.setMetadataPrefix(metadataPrefix);
			break;
		case LISTIDENTIFIERS:
			response = oaipmhService.listIdentifiers(metadataPrefix, resumptionToken);
			break;
		case LISTMETADATAFORMATS:
			response = oaipmhService.listMetadataFormats();
			break;
		}

		response.setRequest(requestFragment);
		return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_XML).body(response);
	}

}
