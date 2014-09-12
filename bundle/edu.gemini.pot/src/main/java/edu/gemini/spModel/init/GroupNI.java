/**
 * $Id: GroupNI.java 46768 2012-07-16 18:58:53Z rnorris $
 */

package edu.gemini.spModel.init;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPFactory;

import edu.gemini.spModel.obscomp.SPGroup;



import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * Initializes <code>{@link edu.gemini.pot.sp.ISPGroup}</code> nodes.
 */
public class GroupNI implements ISPNodeInitializer {
    private static final Logger LOG = Logger.getLogger(edu.gemini.spModel.init.GroupNI.class.getName());

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

        try {
            node.setDataObject(new SPGroup("Group")); 
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Failed to set data object of a group node.", ex);
        }
    }

    /**
     * Updates the given <code>node</code>. This should be called on any new
     * nodes created by making a deep copy of another node, so that the user
     * objects are updated correctly.
     *
     * @param node the science program node to be updated
     */
    public void updateNode(ISPNode node)  {
    }
}
