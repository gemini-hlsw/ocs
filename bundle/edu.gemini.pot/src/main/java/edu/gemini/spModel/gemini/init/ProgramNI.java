// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: ProgramNI.java 46768 2012-07-16 18:58:53Z rnorris $
//
package edu.gemini.spModel.gemini.init;

import edu.gemini.pot.sp.*;

import static edu.gemini.pot.sp.version.JavaVersionMapOps.*;
import edu.gemini.spModel.gemini.obscomp.SPProgram;


/**
 * Initializes <code>{@link ISPProgram}</code> nodes.
 */
public final class ProgramNI implements ISPNodeInitializer {

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
        // The data is stored in an SPProgram object set as the
        node.setDataObject(new SPProgram());
        resetVersionVector(node);
    }

    /**
     * Updates the given <code>node</code>. This should be called on any new
     * nodes created by making a deep copy of another node, so that the user
     * objects are updated correctly.
     *
     * @param node the science program node to be updated
     */
    public void updateNode(ISPNode node)  {
        resetVersionVector(node);
    }

    private void resetVersionVector(ISPNode node) {
        final SPNodeKey k = node.getProgramKey();
        ((ISPProgram) node).setVersions(emptyVersionMap().updated(k, emptyNodeVersions()));
    }
}
