// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: SPTreeListener.java 4336 2004-01-20 07:57:42Z gillies $
//

package jsky.app.ot.viewer;

import java.util.EventListener;

/**
 * Interface implemented by <code>{@link SPTree}</code> listeners
 * to be informed when program nodes are selected.
 */
public interface SPTreeListener extends EventListener {
    public void nodeSelected(SPTreeEvent event);
}

