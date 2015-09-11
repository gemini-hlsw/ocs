// Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: PropertyChangeHandler.java 18053 2009-02-20 20:16:23Z swalker $
//
package edu.gemini.spModel.target.system;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import java.io.Serializable;

/**
 * This class supports the tedious details of keeping tracking of
 * property change listeners and firing events to them.  Subclasses
 * need only call <code>firePropertyEvent</code> when their properties
 * are modified.
 */
public class PropertyChangeHandler
        implements Serializable {
    private transient PropertyChangeSupport _pcSupport;

    /**
     * Provides clone support.
     */
    protected Object clone()
            throws CloneNotSupportedException {
        PropertyChangeHandler result = (PropertyChangeHandler) super.clone();
        result._pcSupport = null;  // need to create a new PropertyChangeSupport
        return result;
    }

    /**
     * Constucts without doing anything.
     */
    protected PropertyChangeHandler() {
    }

    /**
     * Gets the PropertyChangeSupport object, creating it if necessary.
     * This object is used to help fire ordinary property change events to
     * registered listeners.
     */
    protected final PropertyChangeSupport getPropertyChangeSupport() {
        if (_pcSupport == null) {
            _pcSupport = new PropertyChangeSupport(this);
        }
        return _pcSupport;
    }

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        getPropertyChangeSupport().addPropertyChangeListener(pcl);
    }

    public void addPropertyChangeListener(String propertyName,
                                          PropertyChangeListener pcl) {
        getPropertyChangeSupport().addPropertyChangeListener(propertyName, pcl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        getPropertyChangeSupport().removePropertyChangeListener(pcl);
    }

    public void removePropertyChangeListener(String propertyName,
                                             PropertyChangeListener pcl) {
        getPropertyChangeSupport().removePropertyChangeListener(propertyName,
                                                                pcl);
    }

    public boolean hasListeners(String propertyName) {
        return getPropertyChangeSupport().hasListeners(propertyName);
    }

    public void
            firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        getPropertyChangeSupport().firePropertyChange(propertyName,
                                                      oldValue, newValue);
    }

}

