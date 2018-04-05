// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: ProgramNI.java 46768 2012-07-16 18:58:53Z rnorris $
//
package edu.gemini.spModel.gemini.init;

import edu.gemini.pot.sp.*;

import static edu.gemini.pot.sp.version.JavaVersionMapOps.*;

import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.gemini.obscomp.SPProgram;


/**
 * Initializes <code>{@link ISPProgram}</code> nodes.
 */
public enum ProgramNI implements ISPNodeInitializer<ISPProgram, SPProgram> {
    instance;

    @Override
    public SPComponentType getType() {
        return SPComponentType.PROGRAM_BASIC;
    }

    @Override
    public SPProgram createDataObject() {
        return new SPProgram();
    }

    @Override
    public void initNode(ISPFactory factory, ISPProgram node) {
        // The data is stored in an SPProgram object set as the
        node.setDataObject(createDataObject());
        resetVersionVector(node);
    }

    @Override
    public void updateNode(ISPProgram node) {
        resetVersionVector(node);
    }

    private void resetVersionVector(ISPProgram node) {
        final SPNodeKey k = node.getProgramKey();
        node.setVersions(emptyVersionMap().updated(k, emptyNodeVersions()));
    }
}
