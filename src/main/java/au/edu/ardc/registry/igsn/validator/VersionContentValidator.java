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
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.service.VersionService;
import au.edu.ardc.registry.exception.ContentProviderNotFoundException;
import au.edu.ardc.registry.exception.VersionContentAlreadyExisted;

import java.util.Optional;

public class VersionContentValidator {

	private final VersionService versionService;

	private final IdentifierService identifierService;

	private final SchemaService schemaService;

	public VersionContentValidator(IdentifierService identifierService, VersionService versionService,
			SchemaService schemaService) {
		this.versionService = versionService;
		this.identifierService = identifierService;
		this.schemaService = schemaService;
	}

	public boolean isNewContent(String payload) throws VersionContentAlreadyExisted, ContentProviderNotFoundException {
		Schema schema = schemaService.getSchemaForContent(payload);
		FragmentProvider fProvider = (FragmentProvider) MetadataProviderFactory.create(schema, Metadata.Fragment);
		IdentifierProvider iProvider = (IdentifierProvider) MetadataProviderFactory.create(schema, Metadata.Identifier);
		boolean isNewContent = false;
		int numberOfFragments = fProvider.getCount(payload);
		for (int i = 0; i < numberOfFragments; i++) {
			String content = fProvider.get(payload, i);
			String identifier = iProvider.get(content);
			isNewContent = this.isNewContent(content, identifier, schema.getId());
		}
		return isNewContent;
	}

	public boolean isNewContent(String content, String identifier, String schemaID)
			throws VersionContentAlreadyExisted {
		Identifier i = identifierService.findByValueAndType(identifier, Identifier.Type.IGSN);
		if (i == null)
			return true;
		Record record = i.getRecord();
		Optional<Version> cVersion = record.getCurrentVersions().stream()
				.filter(version -> version.getSchema().equals(schemaID)).findFirst();
		return cVersion.map(version -> this.isNewContent(content, version, schemaID)).orElse(true);
	}

	public boolean isNewContent(String content, Version version, String schemaID) throws VersionContentAlreadyExisted {
		String versionHash = version.getHash();
		String incomingHash = versionService.getHash(content);
		if (incomingHash.equals(versionHash)) {
			throw new VersionContentAlreadyExisted(schemaID, versionHash);
		}
		return true;
	}

}
