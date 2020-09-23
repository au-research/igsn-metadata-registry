package au.edu.ardc.registry.igsn.provider.ardcv1;

import au.edu.ardc.registry.common.provider.VisibilityProvider;
import au.edu.ardc.registry.common.util.XMLUtil;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@SuppressWarnings("unused")
public class ARDCv1VisibilityProvider implements VisibilityProvider {

	/**
	 * try to determine if an IGSN record in ARDC v1 schema is visible
	 * @param content The xml content of the ARDC v1 version
	 * @return the first 'isPublic' element's content
	 */
	@Override
	public String get(String content) {
		String isPublic = "true";
		// assume if the isPublic not set or ommited the record is visible
		try {
			NodeList nodeList = XMLUtil.getXPath(content, "//isPublic");
			if (nodeList.getLength() > 0) {
				Node isPublicNode = nodeList.item(0);
				isPublic = isPublicNode.getTextContent();
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		return isPublic;
	}

}