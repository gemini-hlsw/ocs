// Copyright 2003
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: PrintUtil.java 4726 2004-05-14 16:50:12Z brighton $

package jsky.util.gui;

import java.awt.Color;
import java.awt.print.*;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.JComponent;

import jsky.util.I18N;
import jsky.util.Preferences;

import javax.print.attribute.Attribute;


/**
 * Utility class for displaying a print dialog to print a Printable object.
 * This class uses a platform independent dialog and remembers the printer
 * name and settings between sessions.
 */
public class PrintUtil {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(PrintUtil.class);

    // The target window being printed
    private Printable _printable;

    // Base name of file used to store printer settings (under ~/.jsky)
    private static final String _ATTR_FILE = "jsky.printerAttr";

    // The Title for printing
    private String _title;

    // Used to display a print dialog
    private PrinterJob _printerJob;

    // Saves user's printer settings
    private HashPrintRequestAttributeSet _printerAttr;

    // Panel used to display print progress
    private ProgressPanel _progressPanel;

    // If true, print in a background thread
    private boolean _useBgThread = true;


    /**
     * Initialize with the Printable and the title
     */
    public PrintUtil(Printable p) {
        _printable = p;
    }

    /**
     * Initialize with the Printable and the title
     */
    public PrintUtil(Printable p, String title) {
        _printable = p;
        _title = title;
    }

    /**
     * Return the panel used to display print progress.
     * This can be used to add extra feedback while printing.
     */
    public ProgressPanel getProgressPanel() {
        return _progressPanel;
    }

    /**
     * Display a dialog to print the given Printable with the given title
     */
    public static void print(Printable p, String title) {
        try {
            new PrintUtil(p, title).print();
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }

    /**
     * Display a dialog to print the given Printable
     */
    public static void print(Printable p) {
        try {
            new PrintUtil(p).print();
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }

    /**
     * Sets the title to be printed before the table contents.
     *
     * @param    title    title to print before the table
     */
    public void setTitle(String title) {
        _title = title;
    }


    /**
     * Set the default value for the given attribute.
     * For example, to set the default to landscape, pass {@link OrientationRequested}.LANDSCAPE
     */
    public void setAttribute(Attribute attr) {
        _restorePrinterAttr();
        if (!_printerAttr.containsKey(attr.getClass())) {
            _printerAttr.add(attr);
        }
    }


    // By default printing is done in a background thread.
    // This can be used to avoid that.
    public void setUseBgThread(boolean b) {
        _useBgThread = b;
    }

    /**
     * Prints the contents of the target Printable.
     *
     * @param title the title for printing
     * @throws PrinterException thrown if any print-related errors occur
     */
    public void print(String title) throws PrinterException {
        setTitle(title);
        print();
    }

    /**
     * Prints the contents of the target Printable.
     *
     * @throws PrinterException thrown if any print-related errors occur
     */
    public void print() throws PrinterException {
        if (_printerJob == null)
            _printerJob = PrinterJob.getPrinterJob();

        if (_title != null)
            _printerJob.setJobName(_title);
        _printerJob.setPrintable(_printable);

        // restore the user's previous printer selection
        String prefKey = getClass().getName() + ".printer";
        String printer = Preferences.get(prefKey);
        if (printer != null) {
            PrintService[] ar = PrintServiceLookup.lookupPrintServices(null, null);
            for (int i = 0; i < ar.length; i++) {
                if (printer.equals(ar[i].getName())) {
                    _printerJob.setPrintService(ar[i]);
                    break;
                }
            }
        }

        try {
            // restore any the printer attributes from a previous session, if needed
            _restorePrinterAttr();

            if (_printerJob.printDialog(_printerAttr)) {
                // remember the printer name
                PrintService ps = _printerJob.getPrintService();
                if (ps == null)
                    return;
                Preferences.set(prefKey, ps.getName());

                // save the printer attributes for future sessions
                _savePrinterAttr();

                // print the table
                if (_useBgThread) {
                    new PrintWorker().start();
                } else {
                    _printerJob.print(_printerAttr);
                }
            }
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }


    /**
     * Performs all the print calculations in a separate thread.
     * A progress bar is shown to the user while the printing occurs.
     */
    private class PrintWorker extends SwingWorker {

        private Color _bg;

        public PrintWorker() {
            String msg = _I18N.getString("printing");
            if (_progressPanel == null)
                _progressPanel = ProgressPanel.makeProgressPanel(msg);
            else
                _progressPanel.setTitle(msg);
            _progressPanel.start();

            // print with a white background
            if (_printable instanceof JComponent) {
                JComponent c = (JComponent) _printable;
                _bg = c.getBackground();
                if (!_bg.equals(Color.white))
                    c.setBackground(Color.white);
            }
        }

        public Object construct() {
            try {
                _progressPanel.setProgress(5);
                _printerJob.print(_printerAttr);
            } catch (Exception ex) {
                return ex;
            }
            return null;
        }

        public void finished() {
            if (_printable instanceof JComponent) {
                if (!_bg.equals(Color.white)) {
                    JComponent c = (JComponent) _printable;
                    c.setBackground(_bg);
                }
            }

            _progressPanel.stop();

            Object o = getValue();
            if (o instanceof Exception) {
                DialogUtil.error((Exception) o);
            }
        }
    }


    // save the printer attributes for future sessions
    private void _savePrinterAttr() {
        if (_printerAttr != null) {
            try {
                Preferences.getPreferences().serialize(_ATTR_FILE, _printerAttr);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Restore any printer attributes from the previous session
    private void _restorePrinterAttr() {
        if (_printerAttr == null) {
            try {
                _printerAttr = (HashPrintRequestAttributeSet) Preferences.getPreferences().deserialize(_ATTR_FILE);
            } catch (Exception e) {
                _printerAttr = new HashPrintRequestAttributeSet();
            }
        }
    }
}



