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
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.Optional;

public class TableUtil {

    /**
     * Return the cell renderer for the given JTable's header.
     */
    public static TableCellRenderer getHeaderRenderer(final JTable table) {
        try {
            return table.getTableHeader().getDefaultRenderer();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return the cell renderer for the given JTable column.
     */
    public static TableCellRenderer getColumnRenderer(final JTable table, int col) {
        try {
            final TableColumn tc = table.getColumnModel().getColumn(col);
            return Optional.ofNullable(tc.getCellRenderer()).orElse(table.getDefaultRenderer(table.getColumnClass(col)));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Pick good column sizes for the given JTable.
     * Assume that all the columns are to be shown.
     * If all column heads are wider than the column's cells'
     * contents, then you can just use column.sizeWidthToFit().
     *
     * @param table the target JTable
     * @return the sum of all the column widths
     */
    public static int initColumnSizes(final JTable table) {
        return initColumnSizes(table, null);
    }

    /**
     * This method picks good column sizes for the given JTable.
     * If all column heads are wider than the column's cells'
     * contents, then you can just use column.sizeWidthToFit().
     *
     * @param table the target JTable
     * @param show if not null, should be an array with a boolean entry for each column
     *             indicating whether the column should be shown or ignored
     * @return the sum of all the column widths
     */
    public static int initColumnSizes(final JTable table, final boolean[] show) {
        return initColumnSizes(table, Optional.ofNullable(show), 10, true);
    }

    /**
     * This method picks good column sizes for the given JTable.
     * If all column heads are wider than the column's cells'
     * contents, then you can just use column.sizeWidthToFit().
     *
     * @param table the target JTable
     * @param show  if not empty, should be an array with a boolean entry for each column
     *              indicating whether the column should be shown or ignored (otherwise assume all shown)
     * @param padding number of pixels padding to add to calculated cell width
     * @param allowColumnResize true if columns can be resized
     *
     * @return the sum of all the column widths
     */
    public static int initColumnSizes(final JTable table, final Optional<boolean[]> show, final int padding,
                                      final boolean allowColumnResize) {
        final TableModel model = table.getModel();
        final int numCols = model.getColumnCount();
        final int numRows = model.getRowCount();

        int sumColWidths = 0;
        for (int col = 0; col < numCols; col++) {
            final TableColumn column = table.getColumnModel().getColumn(col);

            if (!show.isPresent() || show.get()[col]) {
                final TableCellRenderer cellRenderer   = getColumnRenderer(table, col);
                final TableCellRenderer headerRenderer = Optional.ofNullable(column.getHeaderRenderer()).orElse(cellRenderer);

                // check the header width
                final Component headerComponent = headerRenderer.getTableCellRendererComponent(table, column.getHeaderValue(), false, false, -1, col);
                int cellWidth = headerComponent.getPreferredSize().width;

                // check the rendered width of the widest row
                for (int row = 0; row < numRows; row++) {
                    final Object o = model.getValueAt(row, col);
                    final Component cellComponent = cellRenderer.getTableCellRendererComponent(table, o, false, false, row, col);
                    cellWidth = Math.max(cellWidth, cellComponent.getPreferredSize().width);
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
        System.out.println();
        return sumColWidths;
    }

    /*
     * Return the index of the row containing the longest value in the given column.
     */
    public static int getWidestRow(TableModel model, int col) {
        int widestRow = 0;
        int maxLength = 0;

        final int numRows = model.getRowCount();
        for (int row = 0; row < numRows; row++) {
            final Object o = model.getValueAt(row, col);
            if (o != null) {
                final int length = o.toString().length();
                if (length > maxLength) {
                    maxLength = length;
                    widestRow = row;
                }
            }
        }
        return widestRow;
    }
}
