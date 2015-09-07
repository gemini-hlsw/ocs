// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: PropertyChangeMultiplexer.java 46831 2012-07-18 22:22:12Z rnorris $
//
package jsky.app.ot.util;


import javax.swing.SwingUtilities;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

/**
 * Event multiplexer that broadcasts received events on the UI thread.
 */
public final class PropertyChangeMultiplexer implements PropertyChangeListener {

    private final Set<PropertyChangeListener> listeners = new HashSet<PropertyChangeListener>();

    public synchronized void propertyChange(final PropertyChangeEvent evt) {
        for (final PropertyChangeListener pcl: listeners) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    pcl.propertyChange(evt);
                }
            });
        }
    }

    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        listeners.add(listener);
    }

    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
        listeners.remove(listener);
    }

}
