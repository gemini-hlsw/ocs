package jsky.app.ot.userprefs.model;

import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Some;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A companion object used in implementing preferences objects.  Holds
 * common implementation for keeping up with the current value and notifying
 * listeners of changes.
 */
public final class PreferencesSupport<T extends ExternalizablePreferences> {
    private final String name;
    private final ExternalizablePreferences.Factory<T> factory;
    private final List<PreferencesChangeListener<T>> listeners = new CopyOnWriteArrayList<>();

    private T currentValue;

    /**
     * Constructs the support object with the name of the preferences
     * collection and the factory used for creating new instances.
     */
    public PreferencesSupport(String name, ExternalizablePreferences.Factory<T> factory) {
        this.name    = name;
        this.factory = factory;
    }

    /**
     * Fetches the current value from the database, caching it for future
     * lookups.
     */
    public synchronized T fetch() {
        if (currentValue != null) return currentValue;
        currentValue = PreferencesDatabase.instance.fetch(name, factory);
        return currentValue;
    }

    /**
     * Stores the current value to the database, caching it for future
     * lookups.
     *
     * @return <code>true</code> if successful, <code>false</code> otherwise
     */
    public boolean store(T newValue) {
        // Don't allow null values.
        if (newValue == null) throw new NullPointerException();

        Option<T> oldVal = None.instance();

        boolean res;
        synchronized (this) {
            if (currentValue != null) oldVal = new Some<>(currentValue);
            currentValue = newValue;
            res = PreferencesDatabase.instance.store(name, newValue);
        }

        if (!oldVal.equals(newValue)) fireChangeEvent(oldVal, newValue);
        return res;
    }

    private void fireChangeEvent(Option<T> oldVal, T newVal) {
        final PreferencesChangeEvent<T> evt;
        evt = new PreferencesChangeEvent<>(oldVal, newVal);

        // unsynchronized here, but using CopyOnWriteArrayList and its
        // snapshot iterator
        for (PreferencesChangeListener<T> l : listeners) {
            l.preferencesChanged(evt);
        }
    }

    /**
     * Adds a listener that will receive updates when the current value is
     * updated via {@link #store}
     */
    public synchronized void addChangeListener(PreferencesChangeListener<T> listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }
}
