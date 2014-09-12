//
// $
//

package edu.gemini.spModel.gemini.gems;

import edu.gemini.pot.sp.ISPFactory;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.spModel.config.IConfigBuilder;


import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Initializes {@link edu.gemini.pot.sp.ISPObsComponent} node of type
 * {@link Gems}.
 */
public final class GemsNI implements ISPNodeInitializer {
    private static final Logger LOG = Logger.getLogger(GemsNI.class.getName());

    public void initNode(ISPFactory factory, ISPNode node)  {
        ISPObsComponent castNode = (ISPObsComponent) node;

        if (!castNode.getType().equals(Gems.SP_TYPE)) {
            throw new InternalError();
        }

        try {
            node.setDataObject(new Gems());
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Could not set the Gems data object", ex);
            throw new InternalError();
        }

        updateNode(node);
    }

    public void updateNode(ISPNode node)  {
        node.putClientData(IConfigBuilder.USER_OBJ_KEY, new GemsCB((ISPObsComponent) node));
    }
}
