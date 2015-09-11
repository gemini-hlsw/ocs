// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: OptionWidget.java 6719 2005-11-08 19:35:36Z brighton $
//
/**
 * This class watches a OptionWidget object to know which node is selected.
 *
 * @author      Dayle Kotturi, Shane Walker, Allan Brighton (Swing port)
 * @version     $Version$
 */

package jsky.util.gui;



import javax.swing.JRadioButton;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

/**
 * An OptionWidget that permits clients to register as button press watchers.
 */
public class OptionWidget extends JRadioButton implements ActionListener {

    // Observers
    private Vector _watchers = new Vector();

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

        _watchers.addElement(ow);
    }

    /**
     * Delete a watcher.
     */
    public synchronized final void deleteWatcher(OptionWidgetWatcher ow) {
        _watchers.removeElement(ow);
    }

    /**
     * Delegate this method from the Observable interface.
     */
    public synchronized final void deleteWidgetWatchers() {
        _watchers.removeAllElements();
    }


    //
    // Notify watchers that a button has been pressed in the option widget.
    //
    private void _notifyAction() {
        for (int i = 0; i < _watchers.size(); ++i) {
            OptionWidgetWatcher ow = (OptionWidgetWatcher) _watchers.elementAt(i);
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

