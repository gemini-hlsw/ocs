package jsky.util.gui;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * An CheckBoxWidget that permits clients to register as button press watchers.
 */
public class CheckBoxWidget extends JCheckBox implements ActionListener {

    // Observers
    private List<CheckBoxWidgetWatcher> _watchers = new ArrayList<>();

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

        _watchers.add(cbw);
    }

    /**
     * Delete a watcher.
     */
    public synchronized final void deleteWatcher(CheckBoxWidgetWatcher cbw) {
        _watchers.remove(cbw);
    }

    //
    // Notify watchers that a button has been pressed in the option widget.
    //
    private void _notifyAction() {
        for (CheckBoxWidgetWatcher cbw : _watchers) {
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
        button.addWatcher(cbw -> System.out.println("OK"));

        frame.getContentPane().add(button, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new BasicWindowMonitor());
    }
}


