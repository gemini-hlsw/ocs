package edu.gemini.qpt.ui.util;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class EnumBox<T extends Enum<T>> {
    
    public static final String PROP_VALUE = "value";

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    
    private T value;
    
    public EnumBox(T value) {
        set(value);        
    }    
    
    public synchronized void set(T value) {
        if (value == null) throw new IllegalArgumentException("Value cannot be null.");
        T prev = this.value;
        this.value = value;
        pcs.firePropertyChange(PROP_VALUE, prev, value);
    }
    
    public T get() {
        return value;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }
    
}
