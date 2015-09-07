// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SeqBaseNI.java 46768 2012-07-16 18:58:53Z rnorris $
//
package edu.gemini.spModel.init;

import edu.gemini.pot.sp.ISPFactory;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPSeqComponent;



import edu.gemini.spModel.config.IConfigBuilder;
import edu.gemini.spModel.seqcomp.SeqBase;
import edu.gemini.spModel.seqcomp.SeqBaseCB;
import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * Initializes <code>{@link ISPSeqComponent}</code> nodes.
 */
public class SeqBaseNI implements ISPNodeInitializer {
    private static final Logger LOG = Logger.getLogger(SeqBaseNI.class.getName());

    /** A special instance for use when importing programs from XML */
    public static SeqBaseNI XML_IMPORT_INSTANCE = new SeqBaseNI() {
        protected void addSubnodes(ISPFactory factory, ISPSeqComponent sc) {
        }
    };

    /**
     * Initializes the given <code>node</code>.
     * Implements <code>{@link ISPNodeInitializer}</code>
     *
     * @param factory the factory that may be used to create any required
     * science program nodes
     *
     * @param node the science program node to be initialized
     */
    public void initNode(ISPFactory factory, ISPNode node) {
        ISPSeqComponent castNode = (ISPSeqComponent) node;
        if (!castNode.getType().equals(SeqBase.SP_TYPE)) {
            throw new InternalError();
        }

        try {
            castNode.setDataObject(new SeqBase());
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Failed to set data object of SeqBase node", ex);
        }

        // Add the standard sub-nodes
        addSubnodes(factory, castNode);

        // Set the configuration builder
        updateNode(node);
    }


    /** Add the standard sequence base sub-nodes. */
    protected void addSubnodes(ISPFactory factory, ISPSeqComponent sc) {
    }

    /**
     * Updates the given <code>node</code>. This should be called on any new
     * nodes created by making a deep copy of another node, so that the user
     * objects are updated correctly.
     *
     * @param node the science program node to be updated
     */
    public void updateNode(ISPNode node)  {
        node.putClientData(IConfigBuilder.USER_OBJ_KEY, new SeqBaseCB((ISPSeqComponent) node));
    }
}
