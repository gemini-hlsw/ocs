//
// $
//

package edu.gemini.spModel.gemini.nici;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPFactory;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.spModel.config.IConfigBuilder;
import java.util.logging.Logger;
import java.util.logging.Level;



/**
 * Node initializer for {@link SeqRepeatNiciOffset}.
 */
public final class SeqRepeatNiciOffsetNI implements ISPNodeInitializer {
    private static final Logger LOG = Logger.getLogger(SeqRepeatNiciOffsetNI.class.getName());

    /**
     * Initializes the given <code>node</code>.
     * Implements <code>{@link ISPNodeInitializer}</code>
     *
     * @param factory the factory that may be used to create any required
     * science program nodes
     *
     * @param node the science program node to be initialized
     */
    public void initNode(ISPFactory factory, ISPNode node)
             {

        ISPSeqComponent castNode = (ISPSeqComponent) node;
        if (!castNode.getType().equals(SeqRepeatNiciOffset.SP_TYPE)) {
            throw new InternalError();
        }

        // data object of this Seq Component.
        try {
            castNode.setDataObject(new SeqRepeatNiciOffset());
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Failed to set data object of SeqRepeatOffset node.", ex);
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
                           new SeqRepeatNiciOffsetCB((ISPSeqComponent) node));
    }
}
