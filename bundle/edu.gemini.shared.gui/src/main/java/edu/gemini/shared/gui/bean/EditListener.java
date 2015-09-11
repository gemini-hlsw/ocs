package edu.gemini.shared.gui.bean;

import java.util.EventListener;

/**
 * Edit listeners receive events when the JComponent associated with a
 * PropertyCtrl instance has been modified.  The PropertyCtrl will try to keep
 * the associated bean up-to-date with edits, but not all values that can be
 * represented in the widget are accepted by the underlying model.  For example,
 * instrument coadds do not allow negative values, but the user can type in
 * negative values in the field.  Edit listeners provide a mechanism for getting
 * change information as it applies to the UI component.  If all values are
 * accepted by the bean, then the client could just use property change events
 * on the bean itself.
 *
 * @param <B> bean class
 * @param <T> type of the property being edited
 */
public interface EditListener<B, T> extends EventListener {
    void valueChanged(EditEvent<B, T> event);
}
