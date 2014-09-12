/**
 * $Id: SeqConfigFlamingos2NI.java 46768 2012-07-16 18:58:53Z rnorris $
 */

package edu.gemini.spModel.gemini.flamingos2;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPFactory;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.spModel.config.IConfigBuilder;



/**
 * Initializes <code>{@link edu.gemini.pot.sp.ISPSeqComponent}</code> nodes.
 */
public class SeqConfigFlamingos2NI implements ISPNodeInitializer {

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
        if (!castNode.getType().equals(SeqConfigFlamingos2.SP_TYPE)) {
            throw new InternalError();
        }

        // data object of this Seq Component.
        try {
            castNode.setDataObject(new SeqConfigFlamingos2());
        } catch (Exception ex) {
            System.out.println("Failed to set data object of SeqConfigFlamingos2 node");
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
                           new SeqConfigFlamingos2CB((ISPSeqComponent) node));
    }
}
