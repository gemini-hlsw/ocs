package edu.gemini.qpt.core.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Map;

/**
 * Trivial mutable bean base class that supports transient property change 
 * listeners and has a dirty bit.
 * @author rnorris
 */
public abstract class BaseMutableBean {

    public static final String PROP_DIRTY = "dirty";

    private transient PropertyChangeSupport pcs;
    private transient PropertyChangeSupport cachePcs; // invalidate caches first
    private transient boolean dirty;
    private final Map<String, Map<?, ?>> caches = new HashMap<String, Map<?, ?>>();

    protected BaseMutableBean() {
        pcs = new PropertyChangeSupport(this);
        cachePcs = new PropertyChangeSupport(this);
    }

    ///
    /// DIRTY BIT
    ///

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        boolean prev = this.dirty;
        this.dirty = dirty;
        firePropertyChange(PROP_DIRTY, prev, dirty);
    }

    ///
    /// DELEGATE METHODS
    ///

    public void addPropertyChangeListener(PropertyChangeListener arg0) {
        pcs.addPropertyChangeListener(arg0);
    }

    public void addPropertyChangeListener(String arg0, PropertyChangeListener arg1) {
        pcs.addPropertyChangeListener(arg0, arg1);
    }

    protected void firePropertyChange(String arg0, Object arg1, Object arg2) {
        cachePcs.firePropertyChange(arg0, arg1, arg2);
        pcs.firePropertyChange(arg0, arg1, arg2);
    }

    public void removePropertyChangeListener(PropertyChangeListener arg0) {
        pcs.removePropertyChangeListener(arg0);
    }

    public void removePropertyChangeListener(String arg0, PropertyChangeListener arg1) {
        pcs.removePropertyChangeListener(arg0, arg1);
    }

    ///
    /// CACHE SUPPORT
    ///

    /**
     * Returns an empty map whose content will be cleared whenever any of the
     * specified properties change. Caches will be cleared before events are sent
     * to listeners.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <K, V> Map<K, V> getCache(String name, final String... invalidationProperties) {
        Map ret = caches.get(name);
        if (ret == null) {

            final Map cache = new HashMap();
            if (invalidationProperties.length > 0) {
                cachePcs.addPropertyChangeListener(evt -> {
                    String prop = evt.getPropertyName();
                    for (String iProp: invalidationProperties) {
                        if (prop.equals(iProp)) {
                            synchronized (cache) {
                                cache.clear();
                            }
                        }
                    }
                });
            }
            synchronized (caches) {
                caches.put(name, cache);
            }
            ret = cache;
        }
        return ret;
    }

    public void invalidateAllCaches() {
        synchronized (caches) {
            for (Map<?, ?> map: caches.values()) {
                synchronized (map) {
                    map.clear();
                }
            }
        }
    }

}




