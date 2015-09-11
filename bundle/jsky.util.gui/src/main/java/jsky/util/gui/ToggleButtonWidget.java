// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: ToggleButtonWidget.java 21355 2009-08-04 18:28:07Z swalker $
//
package jsky.util.gui;



import javax.swing.ImageIcon;
import javax.swing.JToggleButton;
import javax.swing.border.BevelBorder;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
    private List<ToggleButtonWidgetWatcher> _watchers = new Vector<ToggleButtonWidgetWatcher>();

    /** If true, multiple buttons may be selected, otherwise only one */
    private boolean enableMultipleSelection;


    /** The default constructor. */
    public ToggleButtonWidget(boolean enableMultipleSelection) {
        super();
        init(enableMultipleSelection);
    }

    /** Constructor with label. */
    public ToggleButtonWidget(String label, boolean enableMultipleSelection) {
        super(label);
        init(enableMultipleSelection);
    }

    /** Constructor with icon. */
    public ToggleButtonWidget(ImageIcon icon, boolean enableMultipleSelection) {
        super(icon);
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

    /**
     * Delete all watchers.
     */
    public synchronized final void deleteWatchers() {
        _watchers.clear();
    }

    //
    // Get a copy of the _watchers Vector.
    //
    private synchronized List _getWatchers() {
        return (List) ((Vector) _watchers).clone();
    }

    /**
     * Notify watchers of an action event.
     */
    public void action() {
        List v = _getWatchers();
        int cnt = v.size();
        for (int i = 0; i < cnt; ++i) {
            ToggleButtonWidgetWatcher watcher;
            watcher = (ToggleButtonWidgetWatcher) v.get(i);
            watcher.toggleButtonAction(this);
        }
    }
}

