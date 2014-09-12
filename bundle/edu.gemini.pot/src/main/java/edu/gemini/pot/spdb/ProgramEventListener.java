// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: ProgramEventListener.java 46971 2012-07-25 16:59:17Z swalker $
//

package edu.gemini.pot.spdb;

import edu.gemini.pot.sp.ISPRootNode;

import java.util.EventListener;

/**
 * Interface implemented by ODB listeners to be kept apprised as programs are
 * added and removed.
 */
public interface ProgramEventListener<N extends ISPRootNode> extends EventListener {
    void programAdded(ProgramEvent<N> pme);

    void programReplaced(ProgramEvent<N> pme);

    void programRemoved(ProgramEvent<N> pme);
}

