package au.edu.ardc.registry.igsn.validator;

import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.service.IdentifierService;
import au.edu.ardc.registry.common.service.RecordService;
import au.edu.ardc.registry.common.service.VersionService;
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

	public boolean isNewContent(String content, String identifier, String schema){
		Identifier i = iService.findByValueAndType(identifier, Identifier.Type.IGSN);
		if(i == null)
			return true;
		Record record = i.getRecord();
		Optional<Version> cVersion = record.getCurrentVersions()
				.stream()
				.filter(version -> version.getSchema().equals(schema))
				.findFirst();
		return cVersion.map(version -> this.isNewContent(content, version)).orElse(true);
	}


	public boolean isNewContent(String content, Version version) {
		String versionHash = version.getHash();
		String incomingHash = vService.getHash(content);
		return !incomingHash.equals(versionHash);
	}

}
