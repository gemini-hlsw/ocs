package jsky.util.gui;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Vector;

/**
 * A non-editable combo box with watchers.
 *
 *
 * @author	Shane Walker, Allan Brighton (Swing port)
 */
public class DropDownListBoxWidget<T> extends JComboBox<T>  {

    // Observers
    private Vector<DropDownListBoxWidgetWatcher> _watchers = new Vector<>();

    /** If true, don't fire any action events */
    protected boolean actionsEnabled = true;


    /** Default Constructor */
    public DropDownListBoxWidget() {
        addActionListener(e -> _notifyAction(getIntegerValue()));
    }


    /**
     * Add a watcher.  Watchers are notified when an item is selected.
     */
    public synchronized final void addWatcher(DropDownListBoxWidgetWatcher watcher) {
        if (_watchers.contains(watcher)) {
            return;
        }
        _watchers.addElement(watcher);
    }

    /**
     * Delete a watcher.
     */
    public synchronized final void deleteWatcher(DropDownListBoxWidgetWatcher watcher) {
        _watchers.removeElement(watcher);
    }

    /**
     * Delete all watchers.
     */
    public synchronized final void deleteWatchers() {
        _watchers.removeAllElements();
    }

    //
    // Get a copy of the _watchers Vector.
    //
    @SuppressWarnings("unchecked")
    private synchronized Vector<DropDownListBoxWidgetWatcher> _getWatchers() {
        return (Vector<DropDownListBoxWidgetWatcher>) _watchers.clone();
    }

    //
    // Notify watchers that an item has been double-clicked.
    //
    private void _notifyAction(int index) {
        if (!actionsEnabled)
            return;

        Vector v = _getWatchers();
        int cnt = v.size();
        for (int i = 0; i < cnt; ++i) {
            DropDownListBoxWidgetWatcher watcher = (DropDownListBoxWidgetWatcher) v.elementAt(i);
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

    /** Add the given object to the list of choices */
    public void addChoice(T o) {
        addItem(o);
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

    /**
     * test main
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame("DropDownListBoxWidget");

        DropDownListBoxWidget<String> ddlbwe = new DropDownListBoxWidget<>();
        ddlbwe.setChoices(new String[]{
            "One", "Two", "Three", "Four", "Five", "Six"
        });
        ddlbwe.setChoices(new String[]{
            "XOne", "XTwo", "XThree", "XFour", "XFive", "XSix"
        });

        ddlbwe.addWatcher((ddlbwe1, index, val) -> System.out.println("dropDownListBoxAction: " + ddlbwe1.getValue()));

        frame.getContentPane().add(ddlbwe, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new BasicWindowMonitor());
    }
}

