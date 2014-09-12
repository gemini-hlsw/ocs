/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: TableUtil.java 18053 2009-02-20 20:16:23Z swalker $
 */

package jsky.util.gui;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;

/**
 * Implements static utility methods for use with JTables.
 *
 * @version $Revision: 18053 $
 * @author Allan Brighton
 */
public class TableUtil {

    /**
     * Return the default cell renderer for the given JTable column.
     */
    public static TableCellRenderer getDefaultRenderer(JTable table, TableColumn column) {
        try {
            return table.getTableHeader().getDefaultRenderer();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * This method picks good column sizes for the given JTable.
     * If all column heads are wider than the column's cells'
     * contents, then you can just use column.sizeWidthToFit().
     *
     * @param table the target JTable
     * @param show if not null, should be an array with a boolean entry for each column
     *             indicating whether the column should be shown or ignored.
     *
     * @return the sum of all the column widths
     */
    public static int initColumnSizes(JTable table, boolean[] show) {
        return initColumnSizes(table, show, 10, true);
    }

    /*
     * This method picks good column sizes for the given JTable.
     * If all column heads are wider than the column's cells'
     * contents, then you can just use column.sizeWidthToFit().
     *
     * @param table the target JTable
     * @param show if not null, should be an array with a boolean entry for each column
     *             indicating whether the column should be shown or ignored.
     * @param padding number of pixels padding to add to calculated cell width
     * @param allowColumnResize true if columns can be resized
     *
     * @return the sum of all the column widths
     */
    public static int initColumnSizes(JTable table, boolean[] show, int padding,
                                      boolean allowColumnResize) {
        TableColumn column;
        Component comp;
        int cellWidth;
        TableModel model = table.getModel();
        int numCols = model.getColumnCount();
        int numRows = model.getRowCount();

        if (show != null && show.length != numCols) show = null;

        int sumColWidths = 0;
        for (int col = 0; col < numCols; col++) {
            column = table.getColumnModel().getColumn(col);

            if (show == null || show[col]) {
                TableCellRenderer defaultRenderer = getDefaultRenderer(table, column);
                TableCellRenderer cellRenderer = column.getCellRenderer();
                if (cellRenderer == null)
                    cellRenderer = defaultRenderer;
                TableCellRenderer headerRenderer = column.getHeaderRenderer();
                if (headerRenderer == null)
                    headerRenderer = defaultRenderer;

                // check the header width
                comp = headerRenderer.getTableCellRendererComponent(table, column.getHeaderValue(), false, false, -1, col);
                cellWidth = comp.getPreferredSize().width;

                // check the rendered width of the widest row
                for (int row = 0; row < numRows; row++) {
                    Object o = model.getValueAt(row, col);
                    comp = cellRenderer.getTableCellRendererComponent(table, o, false, false, row, col);
                    cellWidth = Math.max(cellWidth, comp.getPreferredSize().width);
                }

                cellWidth += padding; // add padding
                sumColWidths += cellWidth;
                column.setPreferredWidth(cellWidth);
                if (allowColumnResize) {
                    column.setMinWidth(5);
                    column.setMaxWidth(1000);
                } else {
                    column.setMinWidth(cellWidth);
                    column.setMaxWidth(cellWidth);
                }
            } else {
                // hide column
                column.setMinWidth(0);
                column.setMaxWidth(0);
                column.setPreferredWidth(0);
            }
        }
        return sumColWidths;
    }

    /*
     * Return the index of the row containing the longest value in the given column.
     */
    public static int getWidestRow(TableModel model, int col) {
        int widestRow = 0;
        int maxLength = 0;
        int numRows = model.getRowCount();
        for (int row = 0; row < numRows; row++) {
            Object o = model.getValueAt(row, col);
            if (o != null) {
                String s = o.toString();
                int length = s.length();
                if (length > maxLength) {
                    maxLength = length;
                    widestRow = row;
                }
            }
        }
        return widestRow;
    }
}



