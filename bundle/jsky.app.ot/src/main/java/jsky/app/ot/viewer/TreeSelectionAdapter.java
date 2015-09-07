// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: TreeSelectionAdapter.java 4336 2004-01-20 07:57:42Z gillies $
//
package jsky.app.ot.viewer;

import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;

import edu.gemini.shared.util.ChangeSupport;

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;

/**
 * This adapter listens to TreeSelectionEvents and forwards them
 * as ChangeEvents.  With adapters such as this, a ChangeListener
 * could be notified of tree selections, table selections, or
 * any number of things.
 */
public class TreeSelectionAdapter extends ChangeSupport
        implements TreeSelectionListener {
    /** Implements TreeSelectionListener */
    public void valueChanged(TreeSelectionEvent e) {
        // Don't care about anything specific in this event except the source.
        // Assumption is that the ultimate receiver just needs to know
        // something happened, not exactly what happened.
        fireChangeEvent(e.getSource());
    }
}
