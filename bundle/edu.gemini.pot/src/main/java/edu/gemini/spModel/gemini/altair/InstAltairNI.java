// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
//
package edu.gemini.spModel.gemini.altair;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPFactory;

import edu.gemini.spModel.config.IConfigBuilder;




/**
 * Initializes <code>{@link ISPObsComponent}</code> nodes of type Altair.
 */
public class InstAltairNI implements ISPNodeInitializer {

    /**
     * Initializes the given <code>node</code>.
     * Implements <code>{@link ISPNodeInitializer}</code>
     *
     * @param factory the factory that may be used to create any required
     * science program nodes
     *
     * @param node the science program node to be initialized
     */
    public void initNode(ISPFactory factory, ISPNode node)  {
        //System.out.println("Initializing a Altair Obs Comp");

        ISPObsComponent castNode = (ISPObsComponent) node;
        if (!castNode.getType().equals(InstAltair.SP_TYPE)) {
            throw new InternalError();
        }

        // Create a new InstAltair object
        try {
            node.setDataObject(new InstAltair());
        } catch (Exception ex) {
            System.out.println("Failed to set data object of InstAltair node");
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
        node.putClientData(IConfigBuilder.USER_OBJ_KEY, new InstAltairCB((ISPObsComponent) node));
    }
}
