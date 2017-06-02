package jsky.image.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.swing.JComponent;

import jsky.coords.WorldCoordinateConverter;
import jsky.coords.WorldCoords;
import jsky.util.I18N;
import jsky.util.Preferences;
import jsky.util.gui.SwingWorker;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.PrintPreview;
import jsky.util.gui.ProgressPanel;

/**
 * Displays a print dialog box for printing the current image display
 * and handles the details of printing the image and graphics.
 */
public class ImagePrintDialog implements Printable, ActionListener {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(ImagePrintDialog.class);

    // Base name of file used to store printer settings (under ~/.jsky)
    private static final String _ATTR_FILE = "ImagePrintDialog.printerAttr";

    // The target image display
    private MainImageDisplay _imageDisplay;

    // Used to display a print dialog
    private PrinterJob _printerJob;

    // Saves user's printer settings
    private HashPrintRequestAttributeSet _printerAttr;

    // Panel used to display print progress
    private ProgressPanel _progressPanel;

    // Font used for printing text (headers and footers)
    private static final Font PRINTING_FONT = Font.decode("SansSerif-8");

    private boolean _newPrint;
    private double _printOffsetX;
    private double _printOffsetY;
    private final DateTimeFormatter _dateFormatter = DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss");


    /**
     * Initialize with the target image display object.
     */
    public ImagePrintDialog(MainImageDisplay imageDisplay) {
        this._imageDisplay = imageDisplay;
    }


    /**
     * Display a preview of the image to be printed in a popup window.
     */
    public void preview() {
        SwingWorker worker = new SwingWorker() {
            public Object construct() {
                try {
                    String title = _imageDisplay.getObjectName();
                    if (title == null)
                        title = _imageDisplay.getFilename();
                    if (title == null)
                        title = _I18N.getString("printPreview");
                    startPrint(_I18N.getString("preparingImage"));
                    return new PrintPreview(ImagePrintDialog.this, ImagePrintDialog.this, title);
                } catch (Exception e) {
                    return e;
                }
            }

            public void finished() {
                _progressPanel.stop();
                _progressPanel.setTitle(_I18N.getString("printingImage"));
                Object o = getValue();
                if (o instanceof Exception) {
                    DialogUtil.error((Exception) o);
                } else if (o instanceof PrintPreview) {
                    PrintPreview pp = (PrintPreview) o;
                    pp.setVisible(true);
                }
            }
        };
        worker.start();
    }

    /**
     * Called for the Print button in the preview window
     */
    public void actionPerformed(ActionEvent ae) {
        try {
            print();
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }


    /**
     * Prints the contents of the current image display image area.
     * Prompts user with standard print dialog boxes first.
     */
    public void print() throws PrinterException {
        // Get a PrinterJob
        if (_printerJob == null)
            _printerJob = PrinterJob.getPrinterJob();

        _printerJob.setJobName(_I18N.getString("imageDisplay"));
        _printerJob.setPrintable(this);

        // restore the user's previous printer selection
        String prefKey = getClass().getName() + ".printer";
        String printer = Preferences.get(prefKey);
        if (printer != null) {
            PrintService[] ar = PrintServiceLookup.lookupPrintServices(null, null);
            for (PrintService anAr : ar) {
                if (printer.equals(anAr.getName())) {
                    _printerJob.setPrintService(anAr);
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

                // print the table (this will call the print method below)
                new PrintWorker().start();
            }
        } catch (Exception e) {
            DialogUtil.error(e);
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


    /**
     * For the Printable interface: Render the image contents onto a
     * printable graphics context.  Provides the ability to print the
     * image canvas contents.
     */
    public int print(Graphics g, PageFormat pf, int pageIndex) throws PrinterException {
        Graphics2D g2d = (Graphics2D) g;
        JComponent canvas = _imageDisplay.getCanvas();
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        if (pageIndex > 0)
            return Printable.NO_SUCH_PAGE;

        boolean progress = true;
        if (_newPrint) {
            // Remember the original clip offset
            _newPrint = false;
            progress = false; // No progress event first time because of irregular clip bounds
            Rectangle r = g2d.getClipBounds();
            if (r != null) {
                _printOffsetX = r.x;
                _printOffsetY = r.y;
            }
        }

        // Compute the scale
        double scale = Math.min((pf.getImageableWidth() - 20) / (double) canvasWidth,
                (pf.getImageableHeight() - 20) / (double) canvasHeight);

        // Draw the footer text
        // Just draws name of first image.
        // Probably should rethink how this works for multiple images.
        // Determine default file name
        String footer = _imageDisplay.getObjectName();
        if (footer == null)
            footer = _imageDisplay.getFilename();
        if (footer == null) {
            if (_imageDisplay.isWCS()) {
                WorldCoordinateConverter wcc = _imageDisplay.getWCS();
                WorldCoords center = new WorldCoords(wcc.getWCSCenter(), wcc.getEquinox());
                footer = center.toString();
            } else {
                footer = _I18N.getString("blankImage");
            }
        }

        FontMetrics metrics = canvas.getFontMetrics(PRINTING_FONT);
        int width = metrics.stringWidth(footer) + 6;
        int height = metrics.getHeight() + 4;
        g2d.setColor(Color.black);
        g2d.setFont(PRINTING_FONT);
        g2d.drawString(footer,
                (float) _printOffsetX,
                (float) (((canvasHeight + height) * scale) + pf.getImageableY()));
        footer = _dateFormatter.format(Instant.now());
        width = metrics.stringWidth(footer) + 6;
        g2d.drawString(footer,
                (float) (_printOffsetX + ((canvasWidth - width) * scale) - 15),
                (float) (((canvasHeight + height) * scale) + pf.getImageableY()));

        // Translate and scale the graphics to fit on the page
        g2d.translate(_printOffsetX, _printOffsetY);
        g2d.scale(scale, scale);

        // Clip the canvas drawing so that none of the Viewable objects are drawn
        // outside of the image area.
        int y = 0;
        int x = 0;
        int h = canvasHeight;
        int w = canvasWidth;

        if (g2d.getClipBounds() != null) {
            x = g2d.getClipBounds().x;
            y = g2d.getClipBounds().y;
            w = g2d.getClipBounds().width;
            h = g2d.getClipBounds().height;

            if (x + w > canvasWidth) {
                w = canvasWidth;
            }
            if (y + h > canvasHeight) {
                h = Math.max(0, canvasHeight - y);
            }
        }
        g2d.setClip(x, y, w, h);

        // Paint canvas objects onto the image.
        _imageDisplay.paintImageAndGraphics(g2d);

        if (progress) {
            int percent = (int) Math.min(100, Math.floor(((double) (y + h) / (double) canvasHeight) * 100.0));
            _progressPanel.setProgress(percent);
        }

        return Printable.PAGE_EXISTS;
    }

    /**
     * Initialize printing.  This method must be called at the beginning of any
     * print operation because the print() method will be called multiple times.
     *
     * @param msg the message for the progress dialog
     */
    public void startPrint(String msg) {
        _newPrint = true;
        _printOffsetX = 0.0;
        _printOffsetY = 0.0;

        if (_progressPanel == null)
            _progressPanel = ProgressPanel.makeProgressPanel(msg);
        else
            _progressPanel.setTitle(msg);
        _progressPanel.start();
    }


    /**
     * Performs all the print calculations in a separate thread.
     * A progress bar is shown to the user while the printing occurs.
     */
    protected class PrintWorker extends SwingWorker {

        public PrintWorker() {
            startPrint(_I18N.getString("printing"));
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
            _progressPanel.stop();

            Object o = getValue();
            if (o instanceof Exception) {
                DialogUtil.error((Exception) o);
            }
        }
    }
}


