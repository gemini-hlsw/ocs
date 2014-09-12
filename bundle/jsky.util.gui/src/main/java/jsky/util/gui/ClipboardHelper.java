/*
 * Copyright 2001 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: ClipboardHelper.java 4414 2004-02-03 16:21:36Z brighton $
 */

package jsky.util.gui;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;


/**
 * Utility shortcuts for accessing the system clipboard.
 * This uses the standard Java APIs.
 *
 * @author Klaas Waslander
 * @author Allan Brighton (added change listeners)
 */
public final class ClipboardHelper {

    private static Object contents = null;

    /** list of listeners for change events */
    private static EventListenerList listenerList = new EventListenerList();


    /** not to be instantiated */
    private ClipboardHelper() {
    }

    /**
     * Set the clipboard contents.
     */
    public static void setClipboard(Object arg) {
        // use JDK clipboard in 1.1 if it is a string
        if (arg instanceof String) {
            try {
                Toolkit toolkit = Toolkit.getDefaultToolkit();
                Clipboard cb = toolkit.getSystemClipboard();
                StringSelection s = new StringSelection((String) arg);
                cb.setContents(s, s);
            } catch (Throwable t) {
                // we're in Communicator or something again....
            }
        }
        contents = arg;
        fireChange();
    }

    /**
     * Get the clipboard contents.
     */
    public static Object getClipboard() {
        return contents;
    }

    /**
     * Register to receive change events from this object whenever the
     * clipboard object is set.
     */
    public static void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    /**
     * Stop receiving change events from this object.
     */
    public static void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    /**
     * Notify any listeners of a change in the clipboard object.
     */
    protected static void fireChange() {
        ChangeEvent changeEvent = new ChangeEvent(new Object());
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                ((ChangeListener) listeners[i + 1]).stateChanged(changeEvent);
            }
        }
    }
}
