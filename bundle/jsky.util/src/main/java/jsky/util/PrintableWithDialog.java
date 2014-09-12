/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: PrintableWithDialog.java 4414 2004-02-03 16:21:36Z brighton $
 */

package jsky.util;

import java.awt.print.PrinterException;


/**
 * An interface for widgets that can pop up a print dialog to send their
 * contents to the printer.
 */
public abstract interface PrintableWithDialog {

    /**
     * Display a print dialog to print the contents of this object.
     */
    public void print() throws PrinterException;
}
