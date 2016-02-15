package jsky.util.gui;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.Optional;
import java.util.stream.IntStream;

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
    public static TableCellRenderer getColumnRenderer(final JTable table, final int col) {
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

        return IntStream.range(0, numCols).map(colIdx -> {
            final TableColumn column = table.getColumnModel().getColumn(colIdx);
            final int columnWidth;

            if (!show.isPresent() || show.get()[colIdx]) {
                final TableCellRenderer cellRenderer = getColumnRenderer(table, colIdx);
                final TableCellRenderer headerRenderer = Optional.ofNullable(column.getHeaderRenderer()).orElse(getHeaderRenderer(table));

                // Check the header width if we can.
                final Component headerComponent = headerRenderer.getTableCellRendererComponent(table, column.getHeaderValue(), false, false, -1, colIdx);
                final int headerWidth           = headerComponent.getPreferredSize().width;

                columnWidth = IntStream.range(0, numRows).map(rowIdx -> {
                    final Object o = model.getValueAt(rowIdx, colIdx);
                    final Component cellComponent = cellRenderer.getTableCellRendererComponent(table, o, false, false, rowIdx, colIdx);
                    return cellComponent.getPreferredSize().width;
                }).reduce(headerWidth, Math::max) + padding;

                column.setPreferredWidth(columnWidth);
                if (allowColumnResize) {
                    column.setMinWidth(5);
                    column.setMaxWidth(1000);
                } else {
                    column.setMinWidth(columnWidth);
                    column.setMaxWidth(columnWidth);
                }
            } else {
                // hide column
                columnWidth = 0;
                column.setMinWidth(0);
                column.setMaxWidth(0);
                column.setPreferredWidth(0);
            }
            return columnWidth;
        }).sum();
    }
}
