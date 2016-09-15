package jsky.app.ot.userprefs.model;

import edu.gemini.shared.util.immutable.Option;

public final class PreferencesChangeEvent<T> {
    private final Option<T> oldValue;
    private final T newValue;

    PreferencesChangeEvent(Option<T> oldValue, T newValue) {
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public Option<T> getOldValue() {
        return oldValue;
    }

    public T getNewValue() {
        return newValue;
    }
}
