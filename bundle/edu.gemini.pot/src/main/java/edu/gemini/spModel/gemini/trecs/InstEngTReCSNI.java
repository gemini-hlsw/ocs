package edu.gemini.spModel.gemini.trecs;

import edu.gemini.pot.sp.ISPFactory;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPNodeInitializer;



import edu.gemini.spModel.config.IConfigBuilder;
import java.util.logging.Logger;
import java.util.logging.Level;

//
// Gemini Observatory/AURA
// $Id: InstEngTReCSNI.java 46768 2012-07-16 18:58:53Z rnorris $
//

public class InstEngTReCSNI implements ISPNodeInitializer {

    private static final Logger LOG = Logger.getLogger(InstEngTReCSNI.class.getName());

    /**
     * Initializes the given <code>node</code>.
     * Implements <code>{@link edu.gemini.pot.sp.ISPNodeInitializer}</code>
     *
     * @param factory the factory that may be used to create any required
     * science program nodes
     *
     * @param node the science program node to be initialized
     */
    public void initNode(ISPFactory factory, ISPNode node)
             {

        ISPObsComponent castNode = (ISPObsComponent) node;
        if (!castNode.getType().equals(InstEngTReCS.SP_TYPE)) {
            throw new InternalError();
        }

        // Create a new InstEngTReCS object
        try {
            node.setDataObject(new InstEngTReCS());
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Failed to set data object of InstEngTReCS node in node initializer.");
        }

        // Set the configuration builder
        updateNode(node);
    }


    /**
     * Updates the given <code>node</code>. This should be called on any new
     * nodes created by making a deep copy of another node, so that the user
     * objects are updated correctly.
     *
     * @param node the science program node to be updated
     */
    public void updateNode(ISPNode node)  {
        // Set the configuration builder
        node.putClientData(IConfigBuilder.USER_OBJ_KEY,
                           new InstEngTReCSCB((ISPObsComponent) node));
    }
}
