package au.edu.ardc.registry.oai.service;

import au.edu.ardc.registry.common.config.ApplicationProperties;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.provider.Metadata;
import au.edu.ardc.registry.common.provider.MetadataProviderFactory;
import au.edu.ardc.registry.common.provider.OAIProvider;
import au.edu.ardc.registry.common.service.RecordService;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.service.VersionService;
import au.edu.ardc.registry.oai.exception.*;
import au.edu.ardc.registry.oai.model.*;
import au.edu.ardc.registry.oai.response.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class OAIPMHService {

	public static final int pageSize = 100;

	public static enum Verb {

		IDENTIFY("Identify"), GETRECORD("GetRecord"), LISTRECORDS("ListRecords"), LISTIDENTIFIERS(
				"ListIdentifiers"), LISTSETS("ListSets"), LISTMETADATAFORMATS("ListMetadataFormats");

		private String inputVerb;

		Verb(String verb) {
			this.inputVerb = verb;
		}

		public String getVerb() {
			return this.inputVerb;
		}

	}

	@Autowired
	RecordService recordService;

	@Autowired
	VersionService versionService;

	@Autowired
	ApplicationProperties applicationProperties;

	@Autowired
	SchemaService schemaService;

	public OAIResponse identify() {
		IdentifyFragment identify = new IdentifyFragment();
		identify.setRepositoryName(applicationProperties.getName());
		return new OAIIdentifyResponse(identify);
	}

	public OAIResponse getRecord(String metadataPrefix, String identifier) {
		if (metadataPrefix == null) {
			throw new BadArgumentException();
		}

		if (!this.isOAIProvider(schemaService.getSchemaByID(metadataPrefix))) {
			throw new CannotDisseminateFormatException();
		}

		if (identifier == null) {
			throw new BadArgumentException();
		}

		try {
			Record record = recordService.findById(identifier);
			Version version = versionService.findVersionForRecord(record, metadataPrefix);
			String content = new String(version.getContent());
			return new GetRecordResponse(record, content);
		}
		catch (Exception e) {
			throw new IdDoesNotExistException();
		}
	}

	public OAIResponse listRecords(String metadataPrefix, String resumptionToken) throws JsonProcessingException {
		if (metadataPrefix == null) {
			throw new BadArgumentException();
		}

		if (!this.isOAIProvider(schemaService.getSchemaByID(metadataPrefix))) {
			throw new CannotDisseminateFormatException();
		}

		OAIListRecordsResponse response = new OAIListRecordsResponse();
		ListRecordsFragment listRecordsFragment = new ListRecordsFragment();

		long cursor = OAIProvider.getCursor(resumptionToken, pageSize);
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
			response.setRecordsFragment(listRecordsFragment);
			if (!versions.isLast()) {
				response.setResumptionToken(String.valueOf(versions.getTotalElements()), String.valueOf(cursor),
						newResumptionToken);
			}
			return response;
		}
		catch (Exception e) {
			throw new NoRecordsMatchException();
		}

	}

	public OAIResponse listIdentifiers(String metadataPrefix, String resumptionToken) throws JsonProcessingException {
		if (metadataPrefix == null) {
			throw new BadArgumentException();
		}

		if (!this.isOAIProvider(schemaService.getSchemaByID(metadataPrefix))) {
			throw new CannotDisseminateFormatException();
		}

		OAIListIdentifiersResponse response = new OAIListIdentifiersResponse();
		ListIdentifiersFragment listIdentifiersFragment = new ListIdentifiersFragment();
		long cursor = OAIProvider.getCursor(resumptionToken, pageSize);
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
			response.setIdentifiersFragment(listIdentifiersFragment);
			if (!versions.isLast()) {
				response.setResumptionToken(String.valueOf(versions.getTotalElements()), String.valueOf(cursor),
						newResumptionToken);
			}
			return response;
		}
		catch (Exception e) {
			throw new NoRecordsMatchException();
		}
	}

	public OAIResponse listMetadataFormats() {
		OAIListMetadataFormatsResponse response = new OAIListMetadataFormatsResponse();
		ListMetadataFormatsFragment metadataFormatsFragment = new ListMetadataFormatsFragment();
		List<Schema> schemas = this.getOAIProviders();
		for (Schema schema : schemas) {
			OAIProvider oaiProvider = (OAIProvider) MetadataProviderFactory.create(schema, Metadata.OAI);
			metadataFormatsFragment.setMetadataFormat(oaiProvider.getPrefix(schema),
					oaiProvider.getFormatSchema(schema), oaiProvider.getNamespace(schema));
		}
		response.setListMetadataFormatsFragment(metadataFormatsFragment);
		return response;
	}

	/**
	 * Checks if the given schema is available for OAI export
	 * @return List of schemas that are able to export to oai
	 */
	public List<Schema> getOAIProviders() {
		List<Schema> oaiSchemas = new ArrayList<Schema>();
		List<Schema> schemas = schemaService.getSchemas();
		for (Schema schema : schemas) {
			if (this.isOAIProvider(schema)) {
				oaiSchemas.add(schema);
			}
		}
		return oaiSchemas;
	}

	/**
	 * Checks if the given schema is available for OAI export
	 * @param schema the schema to be checked
	 * @return Boolean
	 */
	public Boolean isOAIProvider(Schema schema) {
		try {
			schema.getProviders().containsKey(Metadata.OAI);
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

	/**
	 * Checks if the given verb is a valid OAI verb
	 * @param inputVerb the verb to be checked
	 * @return Boolean
	 */
	public Boolean isValidVerb(String inputVerb) {
		for (Verb verb : Verb.values()) {
			if (verb.getVerb().equals(inputVerb))
				return true;
		}
		return false;
	}

}
