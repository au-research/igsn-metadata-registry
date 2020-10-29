package au.edu.ardc.registry.igsn.provider.ardcv1;

import au.edu.ardc.registry.common.provider.EmbargoEndProvider;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.common.util.XMLUtil;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.Date;

public class ARDCv1EmbargoEndProvider implements EmbargoEndProvider {

	/**
	 * retrieve the embargoEnd of an IGSN record in ARDC v1 schema
	 * @param content xml content of the ARDC v1 version
	 * @return The embargoEnd as String if it exists
	 */
	@Override
	public Date get(String content) {
		Date result;
		try {
			NodeList nodeList = XMLUtil.getXPath(content, "//isPublic");
			Element resourceEmbargoEndNode = (Element) nodeList.item(0);
			String embargoEndText = resourceEmbargoEndNode.getAttribute("embargoEnd");
			System.out.println(embargoEndText);
			result = (Helpers.convertDate(embargoEndText));
		} catch (Exception ex) {
			ex.printStackTrace();
			result = null;
		}
		return result;
	}

}
