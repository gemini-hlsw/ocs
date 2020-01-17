package jsky.util.gui;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * A non-editable combo box with watchers.
 *
 *
 * @author	Shane Walker, Allan Brighton (Swing port)
 */
public class DropDownListBoxWidget<T> extends JComboBox<T>  {

    // Observers
    private List<DropDownListBoxWidgetWatcher<T>> _watchers = new ArrayList<>();

    /** If true, don't fire any action events */
    protected boolean actionsEnabled = true;


    /** Default Constructor */
    public DropDownListBoxWidget() {
        addActionListener(e -> _notifyAction(getIntegerValue()));
    }


    /**
     * Add a watcher.  Watchers are notified when an item is selected.
     */
    public synchronized final void addWatcher(DropDownListBoxWidgetWatcher<T> watcher) {
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

    //
    // Get a copy of the _watchers Vector.
    //
    @SuppressWarnings("unchecked")
    private synchronized List<DropDownListBoxWidgetWatcher<T>> _getWatchers() {
        return new ArrayList<>(_watchers);
    }

    //
    // Notify watchers that an item has been double-clicked.
    //
    private void _notifyAction(int index) {
        if (!actionsEnabled)
            return;

        List<DropDownListBoxWidgetWatcher<T>> v = _getWatchers();
        for (DropDownListBoxWidgetWatcher<T> watcher : v) {
            watcher.dropDownListBoxAction(this, index, getStringValue());
        }
    }

    /**
     * Set the index of the selected value.
     */
    public void setValue(int index) {
        actionsEnabled = false;
        setSelectedIndex(index);
        actionsEnabled = true;
    }

    /**
     * Set the selected value.
     */
    public void setValue(Object o) {
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
    public void setChoices(List<T> choices) {
        actionsEnabled = false;
        removeAllItems();
        for (T choice : choices) addItem(choice);
        actionsEnabled = true;
    }

    /** Set the choices by specifying the objects that appear on screen. */
    public void setChoices(T[] choices) {
        actionsEnabled = false;
        removeAllItems();
        for (T choice : choices) addItem(choice);
        actionsEnabled = true;
    }

    /** Clear the list of choices. */
    public void clear() {
        actionsEnabled = false;
        removeAllItems();
        actionsEnabled = true;
    }
}

