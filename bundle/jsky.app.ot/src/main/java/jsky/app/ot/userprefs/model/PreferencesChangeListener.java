package jsky.app.ot.userprefs.model;

import java.util.EventListener;

public interface PreferencesChangeListener<T> extends EventListener {
    void preferencesChanged(PreferencesChangeEvent<T> evt);
}
