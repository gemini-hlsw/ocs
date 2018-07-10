package edu.gemini.qpt.ui.util;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Map;

/**
 * The preference manager is basically a typesafe transient map from preference tokens 
 * to values, with change notifications. Keys must implement the Preference interface, 
 * and values may be anything (although any individual value must match the parameterized
 * type of its key).
 * <p>
 * In order to make this baby persistent (which would be nice) we would need to force the
 * keys and values to be serializable (and then deal with migration issues) or force them
 * to be primitives so we could use the Java preferences API. I don't like either of these
 * constraints.
 * @author rnorris
 */
public class PreferenceManager {

    /**
     * A preference key is simply a structured token with a type parameter and 
     * a default value. The interface is used in the API to make it convenient to
     * use enumerated types as keys; a default implementation is also provided.
     * @author rnorris
     * @param <T>
     */
    public static interface Key<T> {
        
        /**
         * Should return the default preference value for this key.
         * @return
         */
        T getDefaultValue();
        
        /**
         * Should return the unique internal name of this key. This is the property
         * name that is used in PropertyChangeEvents.
         * @return
         */
        String name();
        
        /**
         * Shortcut for PreferenceManager.get(this)
         * @return
         */
        T get();
        
    }
    
    /**
     * A default, trivial implementation of Key.
     * @author rnorris
     * @param <T>
     */
    public static class SimpleKey<T> implements Key<T> {
        
        private final T defaultValue;
        private final String name;
        
        public SimpleKey(final String name, final T defaultValue) {
            this.defaultValue = defaultValue;
            this.name = name;
        }
        
        public String name() {
            return name;
        }
        
        public T getDefaultValue() {
            return defaultValue;
        }

        public T get() {
            return PreferenceManager.get(this);
        }
        
    }
        
    private static final Map<Key<?>, Object> prefs = new HashMap<Key<?>, Object>();
    private static final PropertyChangeSupport pcs = new PropertyChangeSupport(PreferenceManager.class);
    
    /**
     * Sets the value for the specified preference key, firing a PropertyChangeEvent using
     * the key's name and it's previous and new values. Null values aren't really supported,
     * sorry.
     * @param <T>
     * @param key
     * @param value
     */
    public static <T> void set(Key<T> key, T value) {
        T prev = get(key);
        prefs.put(key, value);
        pcs.firePropertyChange(key.name(), prev, value);
    }
    
    /**
     * Returns the value for the specified key, or the key's default value if the 
     * value has never been set. That is, it will only return <code>null</code> if
     * that's the default value specified by the key.
     * @param <T>
     * @param key
     * @return
     */
    @SuppressWarnings("unchecked") 
    public static <T> T get(Key<T> key) {
        if (!prefs.containsKey(key)) {
            return key.getDefaultValue();
        } else {
            return (T) prefs.get(key);
        }
    }

    public static void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public static void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    public static PropertyChangeListener[] getPropertyChangeListeners() {
        return pcs.getPropertyChangeListeners();
    }

    public static PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        return pcs.getPropertyChangeListeners(propertyName);
    }

    public static void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public static void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(propertyName, listener);
    }

    
    
}
