package jsky.util.gui;

import javax.swing.JComboBox;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A non-editable combo box with watchers.
 *
 *
 * @author	Shane Walker, Allan Brighton (Swing port)
 */
public class DropDownListBoxWidget<T> extends JComboBox<T>  {

    // Observers
    private final List<DropDownListBoxWidgetWatcher<T>> _watchers = new ArrayList<>();

    /** If true, don't fire any action events */
    protected boolean actionsEnabled = true;


    /** Default Constructor */
    public DropDownListBoxWidget() {
        addActionListener(e -> _notifyAction(getIntegerValue()));
    }


    /**
     * Add a watcher.  Watchers are notified when an item is selected.
     */
    public synchronized final void addWatcher(final DropDownListBoxWidgetWatcher<T> watcher) {
        if (_watchers.contains(watcher)) {
            return;
        }
        _watchers.add(watcher);
    }

    /**
     * Delete a watcher.
     */
    public synchronized final void deleteWatcher(DropDownListBoxWidgetWatcher<T> watcher) {
        _watchers.remove(watcher);
    }

    /**
     * Get a copy of the _watchers Vector.
     */
    private synchronized List<DropDownListBoxWidgetWatcher<T>> _getWatchers() {
        return new ArrayList<>(_watchers);
    }

    /**
     * Notify watchers that an item has been double-clicked.
     */
    private void _notifyAction(final int index) {
        if (!actionsEnabled)
            return;
        _getWatchers().forEach(w -> w.dropDownListBoxAction(this, index, getStringValue()));
    }

    /**
     * Set the index of the selected value.
     */
    public void setValue(final int index) {
        actionsEnabled = false;
        setSelectedIndex(index);
        actionsEnabled = true;
    }

    /**
     * Set the selected value.
     */
    public void setValue(final Object o) {
        actionsEnabled = false;
        setSelectedItem(o);
        actionsEnabled = true;
    }

    /**
     * Return the (String) value of the selected item.
     */
    public Object getValue() {
        return getSelectedItem();
    }


    /** Return the index of the selected value. */
    public int getIntegerValue() {
        return getSelectedIndex();
    }

    /** Return the selected value. */
    public String getStringValue() {
        return getSelectedItem().toString();
    }

    /** Set the choices by specifying a Vector containing the strings that represent the choices. */
    public void setChoices(final List<T> choices) {
        actionsEnabled = false;
        removeAllItems();
        choices.forEach(this::addItem);
        actionsEnabled = true;
    }

    /** Set the choices by specifying the objects that appear on screen. */
    public void setChoices(final T[] choices) {
        actionsEnabled = false;
        removeAllItems();
        for (final T choice : choices) addItem(choice);
        actionsEnabled = true;
    }

    /** Clear the list of choices. */
    public void clear() {
        actionsEnabled = false;
        removeAllItems();
        actionsEnabled = true;
    }
}

