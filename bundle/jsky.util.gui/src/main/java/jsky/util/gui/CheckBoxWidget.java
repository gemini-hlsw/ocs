package jsky.util.gui;

import javax.swing.JCheckBox;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An CheckBoxWidget that permits clients to register as button press watchers.
 */
public class CheckBoxWidget extends JCheckBox implements ActionListener {

    // Observers
    private final List<CheckBoxWidgetWatcher> _watchers = new ArrayList<>();

    /** Default constructor */
    public CheckBoxWidget() {
        addActionListener(this);
    }

    /** Default constructor */
    public CheckBoxWidget(final String text) {
        this();
        setText(text);
    }

    /**
     * Add a watcher.  Watchers are notified when a button is pressed in the
     * option widget.
     */
    public synchronized final void addWatcher(final CheckBoxWidgetWatcher cbw) {
        if (_watchers.contains(cbw)) {
            return;
        }

        _watchers.add(cbw);
    }

    /**
     * Delete a watcher.
     */
    public synchronized final void deleteWatcher(final CheckBoxWidgetWatcher cbw) {
        _watchers.remove(cbw);
    }

    /**
     * Get a copy of the _watchers Vector.
     */
    private synchronized List<CheckBoxWidgetWatcher> _getWatchers() {
        return new ArrayList<>(_watchers);
    }

    /**
     * Notify watchers that a button has been pressed in the option widget.
     */
    private void _notifyAction() {
        _getWatchers().forEach(cbw -> cbw.checkBoxAction(this));
    }

    /** Called when the button is pressed. */
    public void actionPerformed(final ActionEvent ae) {
        _notifyAction();
    }

    public void setValue(final boolean value) {
        setSelected(value);
    }

    public boolean getBooleanValue() {
        return isSelected();
    }
}


