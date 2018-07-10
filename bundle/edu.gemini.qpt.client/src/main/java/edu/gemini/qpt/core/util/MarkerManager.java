package edu.gemini.qpt.core.util;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.gemini.qpt.core.Marker;
import edu.gemini.qpt.core.Marker.Severity;

/**
 * Container that binds Markers to an owner (which produced the marker) and an object path.
 * @author rnorris
 */
public class MarkerManager {

    public static final String PROP_MARKERS = "markers";

    private final SortedSet<Marker> markers = new TreeSet<Marker>();
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public synchronized void addMarker(boolean qcOnly, Object owner, Severity severity, String text, Object... path) {
        addMarker(new Marker(qcOnly, owner, severity, text, path));
    }
    
    public synchronized void addMarker(Marker marker) {
        List<Marker> prev = Collections.unmodifiableList(new ArrayList<Marker>(markers));
        markers.add(marker);
        pcs.firePropertyChange(PROP_MARKERS, prev, getMarkers());
    }
    
    public synchronized void clearMarkers(Object owner, Object node) {
        List<Marker> prev = Collections.unmodifiableList(new ArrayList<Marker>(markers));
        for (Iterator<Marker> it = markers.iterator(); it.hasNext(); ) {
            Marker m = it.next();
            if (m.getOwner() == owner) for (Object o: m.getPath()) if (o == node) it.remove();
        }
        pcs.firePropertyChange(PROP_MARKERS, prev, getMarkers());
    }

    public synchronized void clearMarkers(Object owner) {
        List<Marker> prev = Collections.unmodifiableList(new ArrayList<Marker>(markers));
        for (Iterator<Marker> it = markers.iterator(); it.hasNext(); ) {
            Marker m = it.next();
            if (m.getOwner() == owner) it.remove();
        }
        pcs.firePropertyChange(PROP_MARKERS, prev, getMarkers());
    }
    
    public synchronized SortedSet<Marker> getMarkers(Object target, boolean transitive) {
        SortedSet<Marker> accum = new TreeSet<Marker>();
        for (Marker m: markers) {
            if (transitive) {
                for (Object o: m.getPath()) { 
                    if (o == target) {                    
                        accum.add(m);
                        break;
                    }
                }
            } else {
                if (m.getTarget() == target) accum.add(m);
            }
        }
        return accum;
    }

    public SortedSet<Marker> getMarkers(Object target) {
        return getMarkers(target, false);
    }
    
    public synchronized SortedSet<Marker> getMarkers() {
        return new TreeSet<Marker>(markers);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }
    
    
    
}



