// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: TextBoxWidget.java 6719 2005-11-08 19:35:36Z brighton $
//
package jsky.util.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;




/**
 * A TextBoxWidget that permits clients to register as key press watchers.
 *
 * @author	Shane Walker, Allan Brighton (Swing port)
 */
public class TextBoxWidget extends JTextField
        implements DocumentListener, ActionListener {
    // Observers
    private Vector _watchers = new Vector();

    // if true, ignore changes in the text box content
    private boolean _ignoreChanges = false;

    /**
     * Like the "tip" but not shown automatically when the mouse rests on
     * the widget.
     * @see #getDescription
     * @see #setDescription
     */
    public String description;


    /** Default constructor */
    public TextBoxWidget() {
        getDocument().addDocumentListener(this);
        addActionListener(this);
    }

    // -- For the DocumentListener interface --

    /**
     * Gives notification that there was an insert into the
     * document. The range given by the DocumentEvent bounds the
     * freshly inserted region.
     */
    public void insertUpdate(DocumentEvent e) {
        if (!_ignoreChanges)
            _notifyKeyPress();
    }

    /**
     * Gives notification that a portion of the document has been
     * removed. The range is given in terms of what the view last saw
     * (that is, before updating sticky positions).
     */
    public void removeUpdate(DocumentEvent e) {
        if (!_ignoreChanges)
            _notifyKeyPress();
    }

    /** Gives notification that an attribute or set of attributes changed. */
    public void changedUpdate(DocumentEvent e) {
    }


    // -- For the ActionListener interface --

    /** Invoked when an action occurs. */
    public void actionPerformed(ActionEvent e) {
        _notifyAction();
    }

    /**
     * Set the description.
     * @see #description
     */
    public void setDescription(String newDescription) {
        description = newDescription;
    }

    /**
     * Get the description.
     * @see #description
     */
    public String getDescription() {
        return description;
    }


    /**
     * Add a watcher.  Watchers are notified when a key is pressed in the
     * text box.
     */
    public synchronized final void addWatcher(TextBoxWidgetWatcher watcher) {
        if (_watchers.contains(watcher)) {
            return;
        }

        _watchers.addElement(watcher);
    }

    /**
     * Delete a watcher.
     */
    public synchronized final void deleteWatcher(TextBoxWidgetWatcher watcher) {
        _watchers.removeElement(watcher);
    }

    /**
     * Delegate this method from the Observable interface.
     */
    public synchronized final void deleteWatchers() {
        _watchers.removeAllElements();
    }

    //
    // Get a copy of the _watchers Vector.
    //
    private synchronized final Vector _getWatchers() {
        return (Vector) _watchers.clone();
    }

    //
    // Notify watchers that a key has been pressed.
    //
    private void _notifyKeyPress() {
        Vector v = _getWatchers();
        int cnt = v.size();
        for (int i = 0; i < cnt; ++i) {
            TextBoxWidgetWatcher watcher = (TextBoxWidgetWatcher) v.elementAt(i);
            try {
                watcher.textBoxKeyPress(this);
            } catch (Exception e) {
                DialogUtil.error(e);
            }
        }
    }

    //
    // Notify watchers that a return key has been pressed in the text box.
    //
    private void _notifyAction() {
        Vector v = _getWatchers();
        int cnt = v.size();
        for (int i = 0; i < cnt; ++i) {
            TextBoxWidgetWatcher watcher = (TextBoxWidgetWatcher) v.elementAt(i);
            watcher.textBoxAction(this);
        }
    }

    /**
     * Get the current value as a double.
     */
    public double getDoubleValue(double def) {
        try {
            return (Double.valueOf(getValue())).doubleValue();
        } catch (Exception ex) {
        }
        return def;
    }

    /**
     * Set the current value as a double.
     */
    public void setValue(double d) {
        setText(String.valueOf(d));
    }

    /**
     * Get the current value as an int.
     */
    public int getIntegerValue(int def) {
        try {
            return Integer.parseInt(getValue());
        } catch (Exception ex) {
        }

        return def;
    }

    /**
     * Set the current value
     */
    public void setText(String s) {
        _ignoreChanges = true;
        try {
            super.setText(s);
        } catch (Exception e) {
            DialogUtil.error(e);
        }
        _ignoreChanges = false;
    }


    /**
     * Set the current value as an int.
     */
    public void setValue(int i) {
        setText(String.valueOf(i));
    }

    /**
     * Set the current value.
     */
    public void setValue(String s) {
        setText(s);
    }

    /**
     * Return the current value.
     */
    public String getValue() {
        return getText();
    }

    /**
     * test main
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame("TextBoxWidget");

        TextBoxWidget tbw = new TextBoxWidget();
        tbw.addWatcher(new TextBoxWidgetWatcher() {
            public void textBoxKeyPress(TextBoxWidget tbwe) {
                System.out.println("textBoxKeyPress: " + tbwe.getValue());
            }

            public void textBoxAction(TextBoxWidget tbwe) {
                System.out.println("textBoxAction: " + tbwe.getValue());
            }
        });

        frame.getContentPane().add(tbw, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new BasicWindowMonitor());
    }
}

