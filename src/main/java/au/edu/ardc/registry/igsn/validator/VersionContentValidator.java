package au.edu.ardc.registry.igsn.validator;

import au.edu.ardc.registry.common.entity.Version;
import org.apache.commons.codec.digest.DigestUtils;

public class VersionContentValidator {

	public boolean isNewContent(String content, Version version) {
		return !DigestUtils.sha1Hex(content).equals(version.getHash());
	}

}
