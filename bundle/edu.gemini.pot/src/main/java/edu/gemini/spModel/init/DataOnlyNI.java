// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: DataOnlyNI.java 46768 2012-07-16 18:58:53Z rnorris $
//
package edu.gemini.spModel.init;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPFactory;

import edu.gemini.spModel.obscomp.SPDataOnly;



import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * Initializes <code>{@link ISPObsComponent}</code> nodes of type SPDataOnly.
 * This node is a hidden data container.
 */
public class DataOnlyNI implements ISPNodeInitializer {
    private static final Logger LOG = Logger.getLogger(DataOnlyNI.class.getName());

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
        //LOG.log(Level.INFO, "Initing DataOnly Node");

        ISPObsComponent castNode = (ISPObsComponent) node;
        if (!castNode.getType().equals(SPDataOnly.SP_TYPE)) {
            throw new InternalError();
        }

        // The data is stored in an SPDataOnly object set as the
        // data object of this ObsComponent.
        try {
            node.setDataObject(new SPDataOnly());
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Failed to set data object of node", ex);
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
