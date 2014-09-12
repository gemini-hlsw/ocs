// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: CheckBoxWidget.java 6719 2005-11-08 19:35:36Z brighton $
//

/**
 * This class watches a CheckBoxWidget object to know which node is selected.
 *
 * @author      Dayle Kotturi, Allan Brighton (Swing Port)
 * @version     1.0, 8/8/97
 */

package jsky.util.gui;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;




/**
 * An CheckBoxWidget that permits clients to register as button press watchers.
 */
public class CheckBoxWidget extends JCheckBox implements ActionListener {

    // Observers
    private Vector _watchers = new Vector();

    /** Default constructor */
    public CheckBoxWidget() {
        addActionListener(this);
    }

    /** Default constructor */
    public CheckBoxWidget(String text) {
        this();
        setText(text);
    }

    /**
     * Add a watcher.  Watchers are notified when a button is pressed in the
     * option widget.
     */
    public synchronized final void addWatcher(CheckBoxWidgetWatcher cbw) {
        if (_watchers.contains(cbw)) {
            return;
        }

        _watchers.addElement(cbw);
    }

    /**
     * Delete a watcher.
     */
    public synchronized final void deleteWatcher(CheckBoxWidgetWatcher cbw) {
        _watchers.removeElement(cbw);
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
            CheckBoxWidgetWatcher cbw = (CheckBoxWidgetWatcher) _watchers.elementAt(i);
            cbw.checkBoxAction(this);
        }
    }

    /** Called when the button is pressed. */
    public void actionPerformed(ActionEvent ae) {
        _notifyAction();
    }

    public void setValue(boolean value) {
        setSelected(value);
    }

    public boolean getBooleanValue() {
        return isSelected();
    }

    /**
     * test main
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame("CheckBoxWidget");

        CheckBoxWidget button = new CheckBoxWidget("Push Me");
        button.addWatcher(new CheckBoxWidgetWatcher() {
            public void checkBoxAction(CheckBoxWidget cbw) {
                System.out.println("OK");
            }
        });

        frame.getContentPane().add(button, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new BasicWindowMonitor());
    }
}


