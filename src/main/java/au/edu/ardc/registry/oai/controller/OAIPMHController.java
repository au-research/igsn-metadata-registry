package au.edu.ardc.registry.oai.controller;

import au.edu.ardc.registry.common.config.ApplicationProperties;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.provider.Metadata;
import au.edu.ardc.registry.common.provider.MetadataProviderFactory;
import au.edu.ardc.registry.common.provider.OAIProvider;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.oai.exception.BadVerbException;
import au.edu.ardc.registry.oai.model.*;
import au.edu.ardc.registry.oai.response.*;
import au.edu.ardc.registry.common.service.RecordService;
import au.edu.ardc.registry.common.service.VersionService;
import au.edu.ardc.registry.oai.service.OAIPMHService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

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

	@Autowired
	private Environment env;

	@GetMapping(value = "", produces = MediaType.APPLICATION_XML_VALUE)
	public ResponseEntity<OAIResponse> handle(HttpServletRequest request, @RequestParam(required = false, defaultValue = "") String verb,
			@RequestParam(required = false) String identifier, @RequestParam(required = false) String metadataPrefix,
											  @RequestParam(required = false) String resumptionToken)
            throws IOException {

		RequestFragment requestFragment = new RequestFragment();
		requestFragment.setValue(request.getRequestURL().toString());
		requestFragment.setVerb(verb);

		OAIResponse response;
		switch (verb) {
			case "Identify":
				response = oaipmhService.identify();

				response.setRequest(requestFragment);
				return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_XML).body(response);
			case "GetRecord":
				response = oaipmhService.getRecord(metadataPrefix, identifier);
				requestFragment.setIdentifier(identifier);
				requestFragment.setMetadataPrefix(metadataPrefix);

				response.setRequest(requestFragment);
				return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_XML).body(response);
			case "ListRecords":
				if (metadataPrefix == null) {
					throw new BadVerbException("Metadata prefix required", "badArgument");
				}
				if (!schemaService.isOAIProvider(schemaService.getSchemaByID(metadataPrefix))) {
					throw new BadVerbException("Metadata prefix '" + metadataPrefix + "' is not supported",
							"cannotDisseminateFormat");
				}
				requestFragment.setMetadataPrefix(metadataPrefix);
				return getRecords(metadataPrefix, requestFragment, resumptionToken);
			case "ListIdentifiers":
				if (metadataPrefix == null) {
					throw new BadVerbException("Metadata prefix required", "badArgument");
				}
				if (!schemaService.isOAIProvider(schemaService.getSchemaByID(metadataPrefix))) {
					throw new BadVerbException("Metadata prefix '" + metadataPrefix + "' is not supported",
							"cannotDisseminateFormat");
				}
				requestFragment.setMetadataPrefix(metadataPrefix);
				return getIdentifiers(metadataPrefix, requestFragment, resumptionToken);
			case "ListMetadataFormats":
				return ListMetadataFormats(requestFragment);
			default:
				throw new BadVerbException("Illegal OAI verb", "badVerb");
		}
	}

	private ResponseEntity<OAIResponse> ListMetadataFormats(RequestFragment requestFragment) {
		OAIListMetadataFormatsResponse response = new OAIListMetadataFormatsResponse();
		ListMetadataFormatsFragment metadataFormatsFragment = new ListMetadataFormatsFragment();
		List<Schema> schemas = schemaService.getOAIProviders();
		for (Schema schema : schemas) {
			OAIProvider oaiProvider = (OAIProvider) MetadataProviderFactory.create(schema, Metadata.OAI);
			metadataFormatsFragment.setMetadataFormat(oaiProvider.getPrefix(schema),
					oaiProvider.getFormatSchema(schema), oaiProvider.getNamespace(schema));
		}
		response.setListMetadataFormatsFragment(metadataFormatsFragment);
		response.setRequest(requestFragment);
		return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_XML).body(response);
	}

	private ResponseEntity<OAIResponse> getRecords(String metadataPrefix, RequestFragment requestFragment, String resumptionToken)
			throws BadVerbException, JsonProcessingException {
		OAIListRecordsResponse response = new OAIListRecordsResponse();
		ListRecordsFragment listRecordsFragment = new ListRecordsFragment();
		int pageSize = 100;
		long cursor =  OAIProvider.getCursor(resumptionToken, pageSize);
		String newResumptionToken = OAIProvider.getResumptionToken(resumptionToken, pageSize);
		Pageable pageable = OAIProvider.getPageable(resumptionToken, pageSize);
		try {
			Page<Version> versions = versionService.findAllCurrentVersionsOfSchema(metadataPrefix, pageable);
			for (Version version : versions) {
				Record record = version.getRecord();
				String content = new String(version.getContent());
				RecordFragment recordFragment = new RecordFragment(record, content);
				listRecordsFragment.setListRecords(recordFragment);
			}
			response.setRequest(requestFragment);
			response.setRecordsFragment(listRecordsFragment);
			if(!versions.isLast()) {
				response.setResumptionToken
						(String.valueOf(versions.getTotalElements()),
								String.valueOf(cursor), newResumptionToken);
			}
		}
		catch(Exception e){
			throw new BadVerbException("The combination of the values of the from, until, " +
					"set and metadataPrefix arguments results in an empty list.", "noRecordsMatch");
		}
		return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_XML).body(response);
	}

	private ResponseEntity<OAIResponse> getIdentifiers
			(String metadataPrefix, RequestFragment requestFragment, String resumptionToken)
			throws BadVerbException, JsonProcessingException {
		OAIListIdentifiersResponse response = new OAIListIdentifiersResponse();
		ListIdentifiersFragment listIdentifiersFragment = new ListIdentifiersFragment();
		int pageSize = 100;
		long cursor =  OAIProvider.getCursor(resumptionToken, pageSize);
		String newResumptionToken = OAIProvider.getResumptionToken(resumptionToken, pageSize);
		Pageable pageable = OAIProvider.getPageable(resumptionToken, pageSize);
		try {
			Page<Version> versions = versionService.findAllCurrentVersionsOfSchema(metadataPrefix, pageable);
			for (Version version : versions) {
				Record record = version.getRecord();
					RecordHeaderFragment headerFragment = new RecordHeaderFragment(record.getId().toString(),
							record.getModifiedAt().toString());
					listIdentifiersFragment.setListIdentifiers(headerFragment);
			}
			response.setRequest(requestFragment);
			response.setIdentifiersFragment(listIdentifiersFragment);
			if(!versions.isLast()) {
				response.setResumptionToken
						(String.valueOf(versions.getTotalElements()),
								String.valueOf(cursor), newResumptionToken);
			}
		}
		catch(Exception e){
			throw new BadVerbException("Records do not exist", "badVerb");
		}
		return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_XML).body(response);
	}
}
