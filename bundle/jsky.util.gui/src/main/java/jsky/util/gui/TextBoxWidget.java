// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: TextBoxWidget.java 6719 2005-11-08 19:35:36Z brighton $
//
package jsky.util.gui;

import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


/**
 * A TextBoxWidget that permits clients to register as key press watchers.
 * Now has support for "done editing", i.e. to watch when editing is complete (which triggers when an action
 * is performed, i.e. enter is hit, or the focus is lost).
 *
 * @author	Shane Walker, Allan Brighton (Swing port)
 */
public class TextBoxWidget extends JTextField {
    // Observers

    final private ArrayList<TextBoxWidgetWatcher> _watchers;

    // if true, ignore changes in the text box content
    private boolean _ignoreChanges = false;

    /** Default constructor */
    public TextBoxWidget() {
        _watchers = new ArrayList<>();

        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(final DocumentEvent e) {
                if (!_ignoreChanges) _notifyKeyPress();
            }

            @Override
            public void removeUpdate(final DocumentEvent e) {
                if (!_ignoreChanges) _notifyKeyPress();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
            }
        });

        addActionListener(e -> {
            _notifyAction();
            _notifyDoneEditing();
        });

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                _notifyDoneEditing();
            }
        });
    }

    /**
     * Add a watcher.  Watchers are notified when a key is pressed in the
     * text box.
     */
    public synchronized final void addWatcher(TextBoxWidgetWatcher watcher) {
        if (_watchers.contains(watcher)) {
            return;
        }
        _watchers.add(watcher);
    }

    /**
     * Delete a watcher.
     */
    public synchronized final void deleteWatcher(TextBoxWidgetWatcher watcher) {
        _watchers.remove(watcher);
    }

    /**
     * Delegate this method from the Observable interface.
     */
    public synchronized final void deleteWatchers() {
        _watchers.clear();
    }

    //
    // Get a copy of the _watchers Vector.
    //
    @SuppressWarnings("unchecked")
    private synchronized ArrayList<TextBoxWidgetWatcher> _getWatchers() {
        return (ArrayList<TextBoxWidgetWatcher>) _watchers.clone();
    }

    //
    // Notify watchers that a key has been pressed.
    //
    private void _notifyKeyPress() {
        _getWatchers().forEach(tbw -> {
            try {
                tbw.textBoxKeyPress(this);
            } catch (Exception e) {
                DialogUtil.error(e);
            }
        });
    }

    //
    // Notify watchers that a return key has been pressed in the text box.
    //
    private void _notifyAction() {
        _getWatchers().forEach(tbw -> tbw.textBoxAction(this));
    }

    private void _notifyDoneEditing() {
        _getWatchers().forEach(tbw -> tbw.textBoxDoneEditing(this));
    }

    /**
     * Get the current value as a double.
     */
    public double getDoubleValue(double def) {
        try {
            return Double.valueOf(getValue());
        } catch (Exception ex) {
            return def;
        }
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
            return def;
        }
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
}

