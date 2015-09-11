// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SPNoteNI.java 46768 2012-07-16 18:58:53Z rnorris $
//
package edu.gemini.spModel.init;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPFactory;
import edu.gemini.pot.sp.SPComponentType;

import edu.gemini.spModel.obscomp.ProgramNote;
import edu.gemini.spModel.obscomp.SPNote;
import edu.gemini.spModel.obscomp.SchedNote;

import java.util.logging.Logger;
import java.util.logging.Level;




/**
 * Initializes <code>{@link ISPObsComponent}</code> nodes of type SpNote.
 */
public class SPNoteNI implements ISPNodeInitializer {
    private static final Logger LOG = Logger.getLogger(SPNoteNI.class.getName());

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

        ISPObsComponent castNode = (ISPObsComponent) node;
        SPComponentType type = castNode.getType();

        try {
            if (type.equals(SPNote.SP_TYPE)) {
                node.setDataObject(new SPNote());
            } else if (type.equals(SchedNote.SP_TYPE)) {
                node.setDataObject(new SchedNote());
            } else if (type.equals(ProgramNote.SP_TYPE)) {
                node.setDataObject(new ProgramNote());
            } else {
                throw new InternalError();
            }

        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Failed to set data object of note node", ex);
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
