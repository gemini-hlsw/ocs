package edu.gemini.shared.gui.bean;

import java.util.EventObject;

/**
 * See {@link EditListener} for more information.
 *
 * @param <B> bean class
 * @param <T> type of the property
 */
public class EditEvent<B, T> extends EventObject {
    private T oldValue;
    private T newValue;

    EditEvent(PropertyCtrl<B, T> source, T oldValue, T newValue) {
        super(source);
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public PropertyCtrl<B, T> getPropertyCtrl() {
        //noinspection unchecked
        return (PropertyCtrl<B, T>) getSource();
    }

    public T getOldValue() {
        return oldValue;
    }

    public T getNewValue() {
        return newValue;
    }
}
