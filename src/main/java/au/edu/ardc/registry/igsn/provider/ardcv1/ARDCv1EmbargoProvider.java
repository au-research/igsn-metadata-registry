package au.edu.ardc.registry.igsn.provider.ardcv1;

import au.edu.ardc.registry.common.provider.EmbargoProvider;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.common.util.XMLUtil;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Date;

public class ARDCv1EmbargoProvider implements EmbargoProvider {

    /**
     * retrieve the embargoEnd of an IGSN record in ARDC v1 schema
     * @param content The xml content of the ARDC v1 version
     * @return The embargoEnd as String if it exists
     */
    @Override
    public Date get(String content) {
        Date embargoEnd = null;
        try {
            NodeList nodeList = XMLUtil.getXPath(content, "//embargoEnd");
            Node resourceTitleNode = nodeList.item(0);
            embargoEnd = Helpers.convertDate(resourceTitleNode.getTextContent());
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        return embargoEnd;
    }
}
