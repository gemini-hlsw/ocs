// Copyright 2003
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: PrintableJTable.java 8145 2007-10-01 16:59:26Z swalker $

package jsky.util.gui;

import jsky.util.I18N;

import javax.swing.*;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.print.attribute.standard.OrientationRequested;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.Vector;


/**
 * A JTable that implements the {@link Printable} interface.
 **/
public class PrintableJTable extends JTable implements Printable {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(PrintableJTable.class);

    // The maximum number of pages for a given print job.
    // This number is computed when a print is started,
    // but defaults to 1.
    private int _maxNumPage = 1;

    // The title for printing
    private String _title = "Table Contents";

    // Helper class
    private PrintUtil _printUtil;


    /** Default Constructor */
    public PrintableJTable() {
        super();
    }

    /** Initialize a printable table with the given number of rows and columns */
    public PrintableJTable(int numRows, int numColumns) {
        super(numRows, numColumns);
    }

    /** Initialize a printable table with the given data and column headers */
    public PrintableJTable(Object[][] rowData, Object[] columnNames) {
        super(rowData, columnNames);
    }

    /** Initialize a printable table with the given table model */
    public PrintableJTable(TableModel dm) {
        super(dm);
    }

    /** Initialize a printable table with the given table and column models */
    public PrintableJTable(TableModel dm, TableColumnModel cm) {
        super(dm, cm);
    }

    /** Initialize a printable table with the given table, column, and selection models */
    public PrintableJTable(TableModel dm, TableColumnModel cm, ListSelectionModel sm) {
        super(dm, cm, sm);
    }

    /** Initialize a printable table with the given row data and column vector */
    public PrintableJTable(Vector rowData, Vector columnNames) {
        super(rowData, columnNames);
    }


    /**
     * Gets the title to be printed before the table contents.
     **/
    public String getTitle() {
        return _title;
    }

    /**
     * Sets the title to be printed before the table contents.
     *
     * @param	title	title to print before the table
     **/
    public void setTitle(String title) {
        _title = title;
    }

    /**
     * Prints the contents of the table, prompting the user with page setup dialogs and such.
     * Prints title string above the table.
     *
     * @param		title				title to print above the table
     * @exception	PrinterException	thrown if any print-related errors occur
     **/
    public void showPrintDialog(String title) throws PrinterException {
        setTitle(title);

        showPrintDialog();
    }

    /**
     * Display a print dialog for printing the contents of this table.
     **/
    public void showPrintDialog() throws PrinterException {
        showPrintDialog(null, false);
    }

    public void showPrintDialog(String title, boolean landscape) throws PrinterException {
        if (title != null) setTitle(title);

        if (_printUtil == null) {
            _printUtil = new PrintUtil(this, _title);
        } else {
            _printUtil.setTitle(_title);
        }

        if (landscape) {
            _printUtil.setAttribute(OrientationRequested.LANDSCAPE);
        } else {
            _printUtil.setAttribute(OrientationRequested.PORTRAIT);
        }

        _printUtil.print();
    }


    /**
     * Modified from
     * <a href="http://developer.java.sun.com/developer/onlineTraining/Programming/JDCBook/advprint.html#pe">example</a>
     * code.
     */
    public int print(Graphics g, PageFormat pageFormat, int pageIndex) throws PrinterException {
        Graphics2D g2 = (Graphics2D) g;
        g2.setBackground(Color.white);
        g2.setColor(Color.black);
        int fontHeight = g2.getFontMetrics().getHeight();
        int fontDesent = g2.getFontMetrics().getDescent();

        //leave room for title and page number
        double pageHeight = pageFormat.getImageableHeight() - (fontHeight * 2);
        double pageWidth = pageFormat.getImageableWidth();
        double tableWidth = (double) getColumnModel().getTotalColumnWidth();
        double scale = 10. / 12.; // use a 10pt font as the default, instead of 12pt
        if (tableWidth >= pageWidth) {
            scale = Math.min(pageWidth / tableWidth, scale);
        }

        double headerHeightOnPage = getTableHeader().getHeight() * scale;
        double tableWidthOnPage = tableWidth * scale;
        double oneRowHeight = getRowHeight() * scale;  // allan: removed "+ margin", since it caused row overlap
        int numRowsOnAPage = (int) ((pageHeight - headerHeightOnPage) / oneRowHeight);
        double pageHeightForTable = oneRowHeight * numRowsOnAPage;
        int totalNumPages = (int) Math.ceil(((double) getRowCount()) / numRowsOnAPage);

        int pageNum = pageIndex + 1;
        ProgressPanel progressPanel = _printUtil.getProgressPanel();
        progressPanel.setProgress((pageNum * 100) / totalNumPages);
        String pageNumInfo = _I18N.getString("pageOf", pageNum, totalNumPages);
        progressPanel.setText(pageNumInfo);

        if (pageIndex >= totalNumPages)
            return NO_SUCH_PAGE;

        g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

        // Draw the title at top center
        String title = getTitle();
        if (title.length() != 0) {
            if (pageNum > 1)
                title += " " + _I18N.getString("cont");
            g2.drawString(title, 0, fontHeight - fontDesent);
        }

        // Draw the page number at the bottom center of each page
        g2.drawString(pageNumInfo,
                      (int) pageWidth / 2 - 35,
                      (int) (pageFormat.getImageableHeight() - fontDesent));

        g2.translate(0, headerHeightOnPage + fontHeight);
        g2.translate(0, -pageIndex * pageHeightForTable);

        //If this piece of the table is smaller than the size available,
        //clip to the appropriate bounds.
        if (pageNum == totalNumPages) {
            int lastRowPrinted = numRowsOnAPage * pageIndex;
            int numRowsLeft = getRowCount() - lastRowPrinted;
            g2.setClip(0,
                       (int) (pageHeightForTable * pageIndex),
                       (int) Math.ceil(tableWidthOnPage),
                       (int) Math.ceil(oneRowHeight * numRowsLeft));
        }
        //else clip to the entire area available.
        else {
            g2.setClip(0,
                       (int) (pageHeightForTable * pageIndex),
                       (int) Math.ceil(tableWidthOnPage),
                       (int) Math.ceil(pageHeightForTable));
        }

        g2.scale(scale, scale);
        paint(g2);
        g2.scale(1 / scale, 1 / scale);
        g2.translate(0, (int) (pageIndex * pageHeightForTable));
        g2.translate(0, (int) -headerHeightOnPage);
        g2.setClip(0, 0, (int) Math.ceil(tableWidthOnPage), (int) Math.ceil(headerHeightOnPage));
        g2.scale(scale, scale);
        getTableHeader().paint(g2); //paint header at top

        return Printable.PAGE_EXISTS;
    }
}

