package au.edu.ardc.registry.oai.controller;

import au.edu.ardc.registry.common.dto.RecordDTO;
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
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.List;

@Controller
@RequestMapping(value = "/api/services/oai-pmh", produces = MediaType.APPLICATION_XML_VALUE)
public class OAIPMHService {

	@Autowired
	SchemaService schemaService;

	@Autowired
	RecordService recordService;

	@Autowired
	VersionService versionService;

	@Autowired
	private Environment env;

	@GetMapping(value = "", produces = MediaType.APPLICATION_XML_VALUE)
	public ResponseEntity<OAIResponse> handle(HttpServletRequest request, @RequestParam(required = false) String verb,
			@RequestParam(required = false) String identifier, @RequestParam(required = false) String metadataPrefix)
			throws JsonProcessingException {

		if (verb == null || verb.equals(""))
			throw new BadVerbException("Illegal OAI verb", "badVerb");

		RequestFragment requestFragment = new RequestFragment();
		requestFragment.setValue(request.getRequestURL().toString());
		requestFragment.setVerb(verb);

		if (verb.equals("Identify")) {
			return identify(requestFragment);
		}
		else if (verb.equals("GetRecord")) {
			if (metadataPrefix == null)
				throw new BadVerbException("Metadata prefix required", "badArgument");
			if (!schemaService.isOAIProvider(schemaService.getSchemaByID(metadataPrefix))) {
				throw new BadVerbException("Metadata prefix " + metadataPrefix + " is not supported",
						"cannotDisseminateFormat");
			}
			if (identifier == null) {
				throw new BadVerbException("Identifier required", "badArgument");
			}
			requestFragment.setIdentifier(identifier);
			requestFragment.setMetadataPrefix(metadataPrefix);
			return getRecord(identifier, metadataPrefix, requestFragment);
		}
		else if (verb.equals("ListRecords")) {
			if (metadataPrefix == null) {
				throw new BadVerbException("Metadata prefix required", "badArgument");
			}
			if (!schemaService.isOAIProvider(schemaService.getSchemaByID(metadataPrefix))) {
				throw new BadVerbException("Metadata prefix '" + metadataPrefix + "' is not supported",
						"cannotDisseminateFormat");
			}
			requestFragment.setMetadataPrefix(metadataPrefix);
			return getRecords(metadataPrefix, requestFragment);
		}
		else if (verb.equals("ListIdentifiers")) {
			if (metadataPrefix == null) {
				throw new BadVerbException("Metadata prefix required", "badArgument");
			}
			if (!schemaService.isOAIProvider(schemaService.getSchemaByID(metadataPrefix))) {
				throw new BadVerbException("Metadata prefix '" + metadataPrefix + "' is not supported",
						"cannotDisseminateFormat");
			}
			requestFragment.setMetadataPrefix(metadataPrefix);
			return getIdentifiers(metadataPrefix, requestFragment);
		}
		else if (verb.equals("ListMetadataFormats")) {
			return ListMetadataFormats(requestFragment);
		}
		else {
			throw new BadVerbException("Illegal OAI verb", "badVerb");
		}
	}

	private ResponseEntity<OAIResponse> identify(RequestFragment requestFragment) {
		IdentifyFragment identify = new IdentifyFragment();
		identify.setRepositoryName(env.getProperty("app.name"));
		OAIResponse response = new OAIIdentifyResponse(identify);
		response.setRequest(requestFragment);
		return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_XML).body(response);
	}

	private ResponseEntity<OAIResponse> getRecord(String identifier, String metadataPrefix,
			RequestFragment requestFragment) {
		try {
			Record record = recordService.findById(identifier);
			try {
				Version version = versionService.findVersionForRecord(record, metadataPrefix);
				String content = new String(version.getContent());
				GetRecordResponse response = new GetRecordResponse(record, content);
				response.setRequest(requestFragment);
				return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_XML).body(response);
			}
			catch (Exception e) {
				throw new BadVerbException("Record with identifier does not exist", "badVerb");
			}
		}
		catch (Exception e) {
			throw new BadVerbException("Record with identifier does not exist", "badVerb");
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

	private ResponseEntity<OAIResponse> getRecords(String metadataPrefix, RequestFragment requestFragment)
			throws BadVerbException {
		OAIListRecordsResponse response = new OAIListRecordsResponse();
		ListRecordsFragment listRecordsFragment = new ListRecordsFragment();
		Pageable pageable = PageRequest.of(0, 100);
		try {
			Page<Record> records = recordService.findAllPublicRecords(pageable);
			for (Record record : records) {
				Version version = versionService.findVersionForRecord(record, metadataPrefix);
				if (version != null) {
					String content = new String(version.getContent());
					RecordFragment recordFragment = new RecordFragment(record, content);
					listRecordsFragment.setListRecords(recordFragment);
				}
			}
		}
		catch (Exception e) {
			throw new BadVerbException("Records do not exist", "badVerb");
		}
		response.setRequest(requestFragment);
		response.setRecordsFragment(listRecordsFragment);
		return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_XML).body(response);
	}

	private ResponseEntity<OAIResponse> getIdentifiers(String metadataPrefix, RequestFragment requestFragment)
			throws BadVerbException {
		OAIListIdentifiersResponse response = new OAIListIdentifiersResponse();
		ListIdentifiersFragment listIdentifiersFragment = new ListIdentifiersFragment();
		Pageable pageable = PageRequest.of(0, 100);
		Page<Record> records = recordService.findAllPublicRecords(pageable);
		for (Record record : records) {
			Version version = versionService.findVersionForRecord(record, metadataPrefix);
			if (version != null) {
				RecordHeaderFragment headerFragment = new RecordHeaderFragment(record.getId().toString(),
						record.getModifiedAt().toString());
				listIdentifiersFragment.setListIdentifiers(headerFragment);
			}
		}
		response.setRequest(requestFragment);
		response.setIdentifiersFragment(listIdentifiersFragment);
		return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_XML).body(response);
	}

}
