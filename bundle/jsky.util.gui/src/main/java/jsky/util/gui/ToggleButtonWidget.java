package jsky.util.gui;

import javax.swing.JToggleButton;
import javax.swing.border.BevelBorder;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

/**
 * A toggle button widget originally based on Bongo, but now ported to Swing.
 *
 * @author Shane Walker, Dayle Kotturi, Allan Brighton (port to Swing)
 */
public class ToggleButtonWidget extends JToggleButton implements ItemListener {

    /** Set to true if the button is depressed */
    protected boolean booleanValue = false;

    // The list of watchers.
    private final List<ToggleButtonWidgetWatcher> _watchers = new Vector<>();

    /** If true, multiple buttons may be selected, otherwise only one */
    private boolean enableMultipleSelection;

    /** Constructor with label. */
    public ToggleButtonWidget(String label, boolean enableMultipleSelection) {
        super(label);
        init(enableMultipleSelection);
    }

    /** Initialize the button */
    protected void init(boolean enableMultipleSelection) {
        this.enableMultipleSelection = enableMultipleSelection;
        addItemListener(this);
        setFocusPainted(false);
        setFont(getFont().deriveFont(Font.PLAIN));
        setBorder(new BevelBorder(BevelBorder.RAISED));
    }

    /** Called when the button is selected or deselected */
    public void itemStateChanged(ItemEvent e) {
        ToggleButtonWidget selectedButton = (ToggleButtonWidget) e.getItem();
        if (e.getStateChange() == ItemEvent.SELECTED) {
            selectedButton.setBorder(new BevelBorder(BevelBorder.LOWERED));
            booleanValue = true;
            action();
        } else {
            selectedButton.setBorder(new BevelBorder(BevelBorder.RAISED));
            booleanValue = false;
            if (ToggleButtonWidget.this.enableMultipleSelection) action();
        }
    }

    /** Return true if the button is selected */
    public boolean getBooleanValue() {
        return booleanValue;
    }

    /**
     * Add a watcher.  Watchers are notified when an item is selected.
     */
    public synchronized final void addWatcher(ToggleButtonWidgetWatcher watcher) {
        if (_watchers.contains(watcher)) return;
        _watchers.add(watcher);
    }

    /**
     * Delete a watcher.
     */
    public synchronized final void deleteWatcher(ToggleButtonWidgetWatcher watcher) {
        _watchers.remove(watcher);
    }

    //
    // Get a copy of the _watchers Vector.
    //
    private synchronized List<ToggleButtonWidgetWatcher> _getWatchers() {
        return Collections.unmodifiableList(_watchers);
    }

    /**
     * Notify watchers of an action event.
     */
    public void action() {
        List<ToggleButtonWidgetWatcher> v = _getWatchers();
        for (ToggleButtonWidgetWatcher watcher: v) {
            watcher.toggleButtonAction(this);
        }
    }
}

