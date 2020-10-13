package au.edu.ardc.registry.igsn.transform.ardcv1;

import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.transform.Transformer;
import au.edu.ardc.registry.common.transform.XSLTransformer;

import java.util.HashMap;

public class ARDCv1ToOAIDCTransformer implements Transformer {

	private static final String path = "xslt/ardc_v1_to_oai_dc.xsl";

	private static final String targetSchemaID = SchemaService.OAIDC;

	@Override
	public Version transform(Version version) {

		String resultDocument = XSLTransformer.transform(path, new String(version.getContent()), new HashMap<>());

		if (resultDocument == null)
			return null;

		Version resultVersion = new Version();
		resultVersion.setSchema(targetSchemaID);
		resultVersion.setCurrent(true);
		resultVersion.setRecord(version.getRecord());
		resultVersion.setContent(resultDocument.getBytes());

		return resultVersion;
	}

}
