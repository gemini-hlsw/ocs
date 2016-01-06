package jsky.util.gui;

import java.awt.*;
import java.util.Vector;
import java.util.HashSet;

import javax.swing.*;

/**
 * A replacement for JComboBox that allows disabling individual items.
 */
public class SingleSelectComboBox<T> extends JComboBox<T> {

    // Set of disabled items
    private HashSet<T> _disabledSet = new HashSet<>();

    // used to enable/disable individual items
    private final ListCellRenderer<Object> _renderer = new DefaultListCellRenderer() {
        @SuppressWarnings("SuspiciousMethodCalls")
        public Component getListCellRendererComponent(final JList<?> list,
                                                      final Object value,
                                                      final int index,
                                                      final boolean isSelected,
                                                      final boolean cellHasFocus) {
            final boolean enabled  = !_disabledSet.contains(value);
            final boolean selected = enabled && isSelected;
            JLabel label = (JLabel) super.getListCellRendererComponent(list,
                                                                       value,
                                                                       index,
                                                                       selected,
                                                                       cellHasFocus);
            label.setEnabled(enabled);
            return label;
        }
    };


    /** Create an empty SingleSelectComboBox */
    public SingleSelectComboBox() {
        super();
        setRenderer(_renderer);
    }

    /** Create a SingleSelectComboBox based on the given model. */
    public SingleSelectComboBox(final ComboBoxModel<T> model) {
        super(model);
        setRenderer(_renderer);
    }

    /** Create a SingleSelectComboBox containing the given items. */
    public SingleSelectComboBox(final T[] ar) {
        super(ar);
        setRenderer(_renderer);
    }

    /** Create a SingleSelectComboBox containing the given items. */
    public SingleSelectComboBox(final Vector<T> v) {
        super(v);
        setRenderer(_renderer);
    }

    /** Set the enabled state of the given item */
    public void setEnabledIndex(final int index, final boolean enabled) {
        setEnabledObject(getModel().getElementAt(index), enabled);
    }

    /** Set the enabled state of the given item */
    public void setEnabledObject(final T o, final boolean enabled) {
        if (enabled) {
            _disabledSet.remove(o);
        } else {
            _disabledSet.add(o);
        }
    }

    /** Set the enabled state of the given item */
    public void setEnabled(final String s, final boolean enabled) {
        final ListModel<T> model = getModel();
        final int n = model.getSize();
        for (int i = 0; i < n; i++) {
            final T o = model.getElementAt(i);
            if (o != null && o.toString().equals(s)) {
                setEnabledObject(o, enabled);
                break;
            }
        }
    }

    /** Set the choices by specifying a Vector containing the choices. */
    public void setChoices(final Vector<T> v) {
        final DefaultComboBoxModel<T> model = new DefaultComboBoxModel<>();
        v.forEach(model::addElement);
        super.setModel(model);
    }

    /** Set the choices to the given objects. */
    public void setChoices(final T[] ar) {
        final DefaultComboBoxModel<T> model = new DefaultComboBoxModel<>();
        for (final T anAr : ar) model.addElement(anAr);
        super.setModel(model);
    }

    /** Return the selected item as a String */
    public String getSelected() {
        final Object o = getSelectedItem();
        if (o != null) {
            return o.toString();
        }
        return null;
    }

    /** Stop actions for disabled items */
    @SuppressWarnings("unchecked")
    protected void fireActionEvent() {
        final T o = (T)getSelectedItem();
        if (!_disabledSet.contains(o)) {
            super.fireActionEvent();
        }
    }

    /** Disallow selecting disabled items */
    public void setSelectedIndex(int anIndex) {
        final T o = getModel().getElementAt(anIndex);
        if (o != null && _disabledSet.contains(o)) {
            return;
        }
        super.setSelectedIndex(anIndex);
    }
}

