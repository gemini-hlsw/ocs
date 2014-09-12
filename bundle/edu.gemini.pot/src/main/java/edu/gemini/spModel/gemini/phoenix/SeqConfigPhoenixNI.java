// Copyright 1997-2002
// Association for Universities for Research in Astronomy, Inc.
//
// $Id: SeqConfigPhoenixNI.java 46768 2012-07-16 18:58:53Z rnorris $
//
package edu.gemini.spModel.gemini.phoenix;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.pot.sp.ISPFactory;

import edu.gemini.spModel.config.IConfigBuilder;




/**
 * Initializes <code>{@link ISPSeqComponent}</code> nodes.
 */
public class SeqConfigPhoenixNI implements ISPNodeInitializer {

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
        //System.out.println("Initializing a Phoenix Seq Comp");

        ISPSeqComponent castNode = (ISPSeqComponent) node;
        if (!castNode.getType().equals(SeqConfigPhoenix.SP_TYPE)) {
            throw new InternalError();
        }

        // data object of this Seq Component.
        try {
            castNode.setDataObject(new SeqConfigPhoenix());
        } catch (Exception ex) {
            System.out.println("Failed to set data object of SeqConfigPhoenix node");
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
                           new SeqConfigPhoenixCB((ISPSeqComponent) node));
    }
}
