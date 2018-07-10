package edu.gemini.qpt.ui.util;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Represents the progress of a multi-step action as a limit, completion amount, message,
 * and cancel request status. This is a controller class that intermediates between client
 * code and a ProgressDialog.
 * @author rnorris
 */
public class ProgressModel {

    public static final String PROP_INDETERMINATE = "indeterminate";
    public static final String PROP_VALUE = "value";
    public static final String PROP_MAX = "max";
    public static final String PROP_MESSAGE = "message";
    public static final String PROP_CANCELLED = "cancelled";
    
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    
    private String message;
    private boolean indeterminate;
    private int value;
    private int max;
    private boolean cancelled;
            
    public ProgressModel(String message, int max) {
        this.message = message;
        this.max = max;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        boolean prev = this.cancelled;
        this.cancelled = cancelled;
        pcs.firePropertyChange(PROP_CANCELLED, prev, cancelled);
    }

    public boolean isIndeterminate() {
        return indeterminate;
    }

    public void setIndeterminate(boolean indeterminate) {        
        boolean prev = this.indeterminate;
        this.indeterminate = indeterminate;
        pcs.firePropertyChange(PROP_INDETERMINATE, prev, indeterminate);
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        int prev = this.max;
        this.max = max;
        pcs.firePropertyChange(PROP_MAX, prev, max);
    }
        
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        String prev = this.message;
        this.message = message;
        pcs.firePropertyChange(PROP_MESSAGE, prev, message);
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        int prev = this.value;
        this.value = value;
        pcs.firePropertyChange(PROP_VALUE, prev, value);
    }

    public void work() {
        setValue(value + 1);
    }
    
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        return pcs.getPropertyChangeListeners();
    }

    public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        return pcs.getPropertyChangeListeners(propertyName);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(propertyName, listener);
    }
    
}
