package jsky.util.gui;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

import javax.swing.event.*;
import javax.swing.border.*;


/**
 * This widget displays a group of radio buttons in a tabular layout.
 */
public class ToggleButtonPanel extends JPanel {

    /** List of listeners for item events */
    protected EventListenerList listenerList = new EventListenerList();

    /** Array of buttons to display */
    JToggleButton[] buttons;

    /** The currently selected button */
    JToggleButton selectedButton;

    /** If true, multiple buttons may be selected, otherwise only one */
    boolean enableMultipleSelection;

    /**
     * Create a panel containing toggle buttons, arranged in the
     * given number of rows and columns.
     *
     * @param icons an array of ToggleButton labels
     * @param nrows the number of rows
     * @param ncols the number of columns
     * @param enableMultipleSelection if true, multiple buttons may be selected, otherwise only one
     * @param hgap the horizontal gap
     * @param vgap the vertical gap
     */
    public ToggleButtonPanel(Icon[] icons, int nrows, int ncols, boolean enableMultipleSelection,
                             int hgap, int vgap) {
        this((Object[])icons, nrows, ncols, enableMultipleSelection, hgap, vgap);
    }

    // Create a panel containing toggle buttons, arranged in the given number of rows and columns.
    // The ids[] array should contain Icons or Strings for the buttons.
    private ToggleButtonPanel(Object[] ids, int nrows, int ncols, boolean enableMultipleSelection,
                             int hgap, int vgap) {
        setLayout(new GridLayout(nrows, ncols, hgap, vgap));
        buttons = new JToggleButton[ids.length];
        ButtonGroup group = new ButtonGroup();
        this.enableMultipleSelection = enableMultipleSelection;

        for (int i = 0; i < ids.length; i++) {
            if (ids[i] == null) {
                add(new JLabel());
            } else {
                buttons[i] = (ids[i] instanceof Icon) ? new JToggleButton((Icon)ids[i])
                        : new JToggleButton(ids[i].toString());
                buttons[i].setFocusPainted(false);
                buttons[i].setFont(buttons[i].getFont().deriveFont(Font.PLAIN));
                buttons[i].setBorder(new BevelBorder(BevelBorder.RAISED));
                buttons[i].addItemListener(e -> {
                    selectedButton = (JToggleButton) e.getItem();
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        selectedButton.setBorder(new BevelBorder(BevelBorder.LOWERED));
                        fireItemEvent(e);
                    } else {
                        selectedButton.setBorder(new BevelBorder(BevelBorder.RAISED));
                        selectedButton = null;
                        if (ToggleButtonPanel.this.enableMultipleSelection)
                            fireItemEvent(e);
                    }
                });

                if (!enableMultipleSelection)
                    group.add(buttons[i]);
                add(buttons[i]);
            }
        }
    }


    /**
     * Register to receive item events from this object whenever the
     * selected item changes. If enableMultipleSelection is true, the
     * listener method is called whenever a button state changes,
     * otherwise only when a button is selected.
     */
    public void addItemListener(ItemListener l) {
        listenerList.add(ItemListener.class, l);
    }

    /**
     * Stop receiving item events from this object.
     */
    public void removeItemListener(ItemListener l) {
        listenerList.remove(ItemListener.class, l);
    }

    /**
     * Notify any item listeners that the selection changed.
     */
    protected void fireItemEvent(ItemEvent itemEvent) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ItemListener.class) {
                ((ItemListener) listeners[i + 1]).itemStateChanged(itemEvent);
            }
        }
    }


    /** Return the selected button */
    public JToggleButton getSelected() {
        return selectedButton;
    }

    /** Return the nth button */
    public JToggleButton getButton(int n) {
        return buttons[n];
    }
}
