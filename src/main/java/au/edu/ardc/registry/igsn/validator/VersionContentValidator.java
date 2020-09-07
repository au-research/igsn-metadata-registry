package au.edu.ardc.registry.igsn.validator;

import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.provider.FragmentProvider;
import au.edu.ardc.registry.common.provider.IdentifierProvider;
import au.edu.ardc.registry.common.provider.Metadata;
import au.edu.ardc.registry.common.provider.MetadataProviderFactory;
import au.edu.ardc.registry.common.service.IdentifierService;
import au.edu.ardc.registry.common.service.RecordService;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.service.VersionService;
import au.edu.ardc.registry.exception.VersionContentAlreadyExisted;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;


public class VersionContentValidator {

	@Autowired
	RecordService rService;

	@Autowired
	VersionService vService;

	@Autowired
	IdentifierService iService;

	@Autowired
	SchemaService sService;

	public boolean isNewContent(String payload) throws VersionContentAlreadyExisted {
		Schema schema = sService.getSchemaForContent(payload);
		FragmentProvider fProvider = (FragmentProvider) MetadataProviderFactory.create(schema, Metadata.Fragment);
		IdentifierProvider iProvider = (IdentifierProvider) MetadataProviderFactory.create(schema, Metadata.Identifier);
		boolean isNewContent = false;
		assert fProvider != null;
		int numberOfFragments = fProvider.getCount(payload);
		for(int i = 0 ; i < numberOfFragments; i++){
			String content = fProvider.get(payload, i);
			assert iProvider != null;
			String identifier = iProvider.get(content);
			isNewContent = this.isNewContent(content, identifier, schema.getId());
		}
		return isNewContent;
	}


	public boolean isNewContent(String content, String identifier, String schemaID) throws VersionContentAlreadyExisted{
		Identifier i = iService.findByValueAndType(identifier, Identifier.Type.IGSN);
		if(i == null)
			return true;
		Record record = i.getRecord();
		Optional<Version> cVersion = record.getCurrentVersions()
				.stream()
				.filter(version -> version.getSchema().equals(schemaID))
				.findFirst();
		return cVersion.map(version -> this.isNewContent(content, version, schemaID)).orElse(true);
	}


	public boolean isNewContent(String content, Version version, String schemaID) throws VersionContentAlreadyExisted{
		String versionHash = version.getHash();
		String incomingHash = vService.getHash(content);
		if(incomingHash.equals(versionHash)){
			throw new VersionContentAlreadyExisted(schemaID, versionHash);
		}
		return true;
	}

}
