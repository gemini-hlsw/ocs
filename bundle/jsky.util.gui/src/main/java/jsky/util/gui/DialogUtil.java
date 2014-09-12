/*
 * ESO Archive
 *
 * $Id: DialogUtil.java 27486 2010-10-18 12:55:54Z swalker $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 */

package jsky.util.gui;

import javax.swing.*;
import java.awt.*;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class with static methods for commonly used dialogs.
 */
public class DialogUtil {
    private static final Logger LOG = Logger.getLogger(DialogUtil.class.getName());

    // Set if using internal frames.
    private static JDesktopPane _desktop;

    /** This should be called if you want to use internal dialogs */
    public static void setDesktop(JDesktopPane d) {
        _desktop = d;
    }

    /** This should be called if you want to use internal dialogs */
    public static JDesktopPane getDesktop() {
        return _desktop;
    }

    /**
     * Report an error message.
     *
     * @param parentComponent display the dialog over the given component
     * @param msg the error message
     */
    public static void error(Component parentComponent, String msg) {
        if (_desktop != null) {
            BusyWin.setBusy(false);
            JOptionPane.showInternalMessageDialog(_desktop, msg, "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(parentComponent, msg, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Report an error message.
     *
     * @param msg the error message
     */
    public static void error(String msg) {
        error(null, msg);
    }


    /**
     * Report an error message based on the given exception.
     *
     * @param parentComponent display the dialog over the given component
     * @param e the exception containing the error information
     */
    public static void error(Component parentComponent, Exception e) {
        LOG.log(Level.WARNING, e.getMessage(), e);

        String s = e.getMessage();
        if (e instanceof UnknownHostException) {
            s = "Unknown host: " + s;
        } else if (s == null || s.trim().length() == 0) {
            s = e.toString();
        }

        if (_desktop != null) {
            BusyWin.setBusy(false);
            JOptionPane.showInternalMessageDialog(_desktop, s, "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(parentComponent, s, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    /**
     * Report an error message based on the given exception.
     *
     * @param e the exception containing the error information
     */
    public static void error(Exception e) {
        error(null, e);
    }


    /**
     * Display an informational message.
     *
     * @param parentComponent display the dialog over the given component
     * @param msg the message
     */
    public static void message(Component parentComponent, String msg) {
        if (_desktop != null) {
            BusyWin.setBusy(false);
            JOptionPane.showInternalMessageDialog(_desktop, msg, "Message", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(parentComponent, msg, "Message", JOptionPane.INFORMATION_MESSAGE);
        }
    }


    /**
     * Display an informational message.
     *
     * @param msg the message
     */
    public static void message(String msg) {
        message(null, msg);
    }


    /**
     * Get an input string from the user and return it.
     *
     * @param parentComponent display the dialog over the given component
     * @param msg the message to display
     * @param initialValue the initial value to display
     * @return the value typed in by the user, or null if Cancel was pressed
     */
    public static String input(Component parentComponent, String msg, String initialValue) {
        if (_desktop != null) {
            BusyWin.setBusy(false);
            return (String) JOptionPane.showInternalInputDialog(_desktop, msg, "Input", JOptionPane.QUESTION_MESSAGE,
                                                                null, null, initialValue);
        } else {
            return JOptionPane.showInputDialog(parentComponent, msg, initialValue);
        }
    }

    /**
     * Get an input string from the user and return it.
     *
     * @param parentComponent display the dialog over the given component
     * @param msg the message to display
     * @return the value typed in by the user, or null if Cancel was pressed
     */
    public static String input(Component parentComponent, String msg) {
        if (_desktop != null) {
            BusyWin.setBusy(false);
            return JOptionPane.showInternalInputDialog(_desktop, msg, "Input", JOptionPane.QUESTION_MESSAGE);
        } else {
            return JOptionPane.showInputDialog(parentComponent, msg, "Input", JOptionPane.QUESTION_MESSAGE);
        }
    }

    /**
     * Get an input string from the user and return it.
     *
     * @param msg the message to display
     * @return the value typed in by the user, or null if Cancel was pressed
     */
    public static String input(String msg) {
        return input(null, msg);
    }

    /**
     * Get a choice from the user and return it.
     *
     * @param parentComponent display the dialog over the given component
     * @param msg the message to display
     * @param choices an array of items to choose from
     * @param initialValue the initial value to display
     * @return the value chosen by the user, or null if Cancel was pressed
     */
    public static Object input(Component parentComponent, String msg, Object[] choices, Object initialValue) {
        if (_desktop != null) {
            BusyWin.setBusy(false);
            return JOptionPane.showInternalInputDialog(_desktop, msg, "Input", JOptionPane.QUESTION_MESSAGE,
                                                       null, choices, initialValue);
        } else {
            return JOptionPane.showInputDialog(parentComponent, msg, "Input", JOptionPane.QUESTION_MESSAGE,
                                               null, choices, initialValue);
        }
    }


    /**
     * Display a confirm dialog with YES, NO, CANCEL buttons and return
     * a JOptionPane constant indicating the choice.
     *
     * @param parentComponent display the dialog over the given component
     * @param msg the message to display
     * @return a JOptionPane constant indicating the choice
     */
    public static int confirm(Component parentComponent, String msg) {
        if (_desktop != null) {
            BusyWin.setBusy(false);
            return JOptionPane.showInternalConfirmDialog(_desktop, msg);
        } else {
            return JOptionPane.showConfirmDialog(parentComponent, msg);
        }
    }

    /**
     * Display a confirm dialog with YES, NO, CANCEL buttons and return
     * a JOptionPane constant indicating the choice.
     *
     * @param msg the message to display
     * @return a JOptionPane constant indicating the choice
     */
    public static int confirm(String msg) {
        return confirm(null, msg);
    }
}


