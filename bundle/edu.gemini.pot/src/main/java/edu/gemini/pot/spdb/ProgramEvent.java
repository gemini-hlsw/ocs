// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: ProgramEvent.java 46971 2012-07-25 16:59:17Z swalker $
//

package edu.gemini.pot.spdb;

import edu.gemini.pot.sp.ISPRootNode;

import java.util.EventObject;

/**
 * The event class fired by the <code>ProgramManager</code> when a program
 * is added or removed.
 */
public final class ProgramEvent<N extends ISPRootNode> extends EventObject {
    private final N _old;
    private final N _new;

    /**
     * Constructs with the <code>ProgramManager</code> reference and the
     * program that was added or removed.
     */
    public ProgramEvent(Object source, N oldRoot, N newRoot) {
        super(source);
        _old = oldRoot;
        _new = newRoot;
    }

    /**
     * Gets the program that was removed or which has been replaced by a newer
     * version.  If this event was fired because a program as been added,
     * <code>null</code> is returned.
     */
    public N getOldProgram() { return _old; }

    /**
     * Gets the program that was added or the new version of a program that
     * has been replaced.  If this event was fired because a program has been
     * removed, <code>null</code> is returned.
     */
    public N getNewProgram() { return _new; }
}

