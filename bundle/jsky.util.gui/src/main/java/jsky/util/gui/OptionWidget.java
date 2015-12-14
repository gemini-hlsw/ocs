package jsky.util.gui;

import javax.swing.JRadioButton;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * An OptionWidget that permits clients to register as button press watchers.
 */
public class OptionWidget extends JRadioButton implements ActionListener {

    // Observers
    private List<OptionWidgetWatcher> _watchers = new ArrayList<>();

    /** Default constructor */
    public OptionWidget() {
        addActionListener(this);
    }

    /**
     * Add a watcher.  Watchers are notified when a button is pressed in the
     * option widget.
     */
    public synchronized final void addWatcher(OptionWidgetWatcher ow) {
        if (_watchers.contains(ow)) {
            return;
        }
        _watchers.add(ow);
    }

    /**
     * Delete a watcher.
     */
    public synchronized final void deleteWatcher(OptionWidgetWatcher ow) {
        _watchers.remove(ow);
    }

    //
    // Notify watchers that a button has been pressed in the option widget.
    //
    private void _notifyAction() {
        for (OptionWidgetWatcher ow : _watchers) {
            ow.optionAction(this);
        }
    }

    /** Called when the button is pressed */
    public void actionPerformed(ActionEvent ae) {
        _notifyAction();
    }


    public void setValue(boolean value) {
        setSelected(value);
    }

    public boolean getValue() {
        return isSelected();
    }
}

