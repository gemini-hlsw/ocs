//
// $
//

package edu.gemini.spModel.gemini.gsaoi;

import edu.gemini.pot.sp.ISPFactory;
import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.spModel.config.IConfigBuilder;



/**
 * Initializes GsaoiSeqConfig nodes.
 */
public final class GsaoiSeqConfigNI implements ISPNodeInitializer {
    public void initNode(ISPFactory factory, ISPNode node)  {
        ISPSeqComponent castNode = (ISPSeqComponent) node;
        if (!castNode.getType().equals(GsaoiSeqConfig.SP_TYPE)) throw new InternalError();

        castNode.setDataObject(new GsaoiSeqConfig());
        updateNode(node);

    }

    public void updateNode(ISPNode node)  {
        node.putClientData(IConfigBuilder.USER_OBJ_KEY,
                           new GsaoiSeqConfigCB((ISPSeqComponent) node));
    }
}
