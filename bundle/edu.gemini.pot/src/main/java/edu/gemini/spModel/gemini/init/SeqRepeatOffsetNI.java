// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: SeqRepeatOffsetNI.java 46768 2012-07-16 18:58:53Z rnorris $
//
package edu.gemini.spModel.gemini.init;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.pot.sp.ISPFactory;

import edu.gemini.spModel.config.IConfigBuilder;

import edu.gemini.spModel.gemini.seqcomp.SeqRepeatOffset;
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatOffsetCB;

import java.util.logging.Logger;
import java.util.logging.Level;




/**
 * Initializes <code>{@link ISPSeqComponent}</code> nodes.
 */
public class SeqRepeatOffsetNI implements ISPNodeInitializer {
    private static final Logger LOG = Logger.getLogger(SeqRepeatOffsetNI.class.getName());

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
        //LOG.log(Level.INFO, "Initing SeqRepeatOffset Node");

        ISPSeqComponent castNode = (ISPSeqComponent) node;
        if (!castNode.getType().equals(SeqRepeatOffset.SP_TYPE)) {
            throw new InternalError();
        }

        // data object of this Seq Component.
        try {
            castNode.setDataObject(new SeqRepeatOffset());
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
                           new SeqRepeatOffsetCB((ISPSeqComponent) node));
    }
}
