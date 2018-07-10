package edu.gemini.qpt.core.listeners;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import edu.gemini.qpt.core.util.BaseMutableBean;
import edu.gemini.qpt.core.util.MarkerManager;

/**
 * Model listener implementation for a listener that wants to create markers.
 * @author rnorris
 */
abstract class MarkerModelListener<T extends BaseMutableBean> implements ModelListener<T>, PropertyChangeListener {
    
    public void subscribe(T t) {
        t.addPropertyChangeListener(this);        
        this.propertyChange(new PropertyChangeEvent(t, null, null, null));
    }
    
    public void unsubscribe(T t) {
        t.removePropertyChangeListener(this);
        getMarkerManager(t).clearMarkers(this, t);
    }
    
    protected abstract MarkerManager getMarkerManager(T t);
    
}


