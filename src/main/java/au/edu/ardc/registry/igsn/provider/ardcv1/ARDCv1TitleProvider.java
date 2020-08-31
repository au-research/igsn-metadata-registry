package au.edu.ardc.registry.igsn.provider.ardcv1;

import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.provider.TitleProvider;
import au.edu.ardc.registry.common.util.XMLUtil;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ARDCv1TitleProvider implements TitleProvider {

    @Override
    public String get(Schema Schema, String content) {
        String title = null;
        try {
            NodeList nodeList = XMLUtil.getXPath(content, "//resourceTitle");
            Node resourceTitleNode = nodeList.item(0);
            title = resourceTitleNode.getTextContent();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        return title;
    }

}
