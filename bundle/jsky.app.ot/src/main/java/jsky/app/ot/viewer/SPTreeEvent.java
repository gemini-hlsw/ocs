// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: SPTreeEvent.java 4336 2004-01-20 07:57:42Z gillies $
//

package jsky.app.ot.viewer;

import java.util.EventObject;


/**
 * The event class fired by the <code>SPTree</code> when a program
 * node is clicked on.
 */
public final class SPTreeEvent extends EventObject {

    /** Describes the selected node */
    private NodeData _viewable;

    /**
     * Initialize the event with the source tree and an object
     * describing the selected node.
     */
    public SPTreeEvent(SPTree source, NodeData viewable) {
        super(source);
        _viewable = viewable;
    }

    /**
     * Return an object describing the selected node.
     */
    public NodeData getViewable() {
        return _viewable;
    }
}

