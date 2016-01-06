package jsky.util.gui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.*;


/**
 * A table cell renderer for multi-line text fields.
 */
public class JTextAreaCellRenderer extends JTextArea implements TableCellRenderer {

    private static final int ROW_PAD = 3;

    protected static final Border NO_FOCUS_BORDER = new EmptyBorder(3, 3, 3, 3);
    protected static final Border FOCUS_BORDER = new CompoundBorder(
            UIManager.getBorder("Table.focusCellHighlightBorder"),
            new EmptyBorder(2, 2, 2, 2));

    private Color unselectedForeground;
    private Color unselectedBackground;

    public JTextAreaCellRenderer() {
        setWrapStyleWord(true);
        setLineWrap(true);
        setOpaque(true);
        setBorder(NO_FOCUS_BORDER);
    }

    public Component getTableCellRendererComponent(final JTable table, final Object value,
                                                   final boolean isSelected, final boolean hasFocus,
                                                   final int row, final int column) {
        if (isSelected) {
            super.setForeground(table.getSelectionForeground());
            super.setBackground(table.getSelectionBackground());
        } else {
            super.setForeground((unselectedForeground != null) ?
                    unselectedForeground : table.getForeground());
            super.setBackground((unselectedBackground != null) ?
                    unselectedBackground : table.getBackground());
        }

        setFont(table.getFont());

        if (hasFocus) {
            setBorder(FOCUS_BORDER);
            if (table.isCellEditable(row, column)) {
                super.setForeground(UIManager.getColor("Table.focusCellForeground"));
                super.setBackground(UIManager.getColor("Table.focusCellBackground"));
            }
        } else {
            setBorder(NO_FOCUS_BORDER);
        }

        setText((value == null) ? "" : value.toString());
        _updateRowHeight(row, column, table);

        return this;
    }

    private void _updateRowHeight(final int row, final int column, final JTable table) {
        if (row < 0 || column < 0) {
            return;
        }

        final int tableColWidth = table.getColumnModel().getColumn(column).getWidth();
        Dimension d = getPreferredSize();
        if (d.width != tableColWidth) {
            setSize(new Dimension(tableColWidth, 1000));
            d = getPreferredSize();
        }

        final int tableRowHeight = table.getRowHeight(row);
        final int defaultTableRowHeight = table.getRowHeight();
        if (d.height < defaultTableRowHeight) {
            d.height = defaultTableRowHeight;
        }

        if (d.height+ROW_PAD != tableRowHeight) {
            table.setRowHeight(row, d.height+ROW_PAD);
        }
    }

    /**
     * Notification from the <code>UIManager</code> that the look and feel
     * [L&F] has changed.
     * Replaces the current UI object with the latest version from the
     * <code>UIManager</code>.
     *
     * @see JComponent#updateUI
     */
    public void updateUI() {
        super.updateUI();
        setForeground(null);
        setBackground(null);
    }

    /**
     * Overrides <code>JComponent.setForeground</code> to assign
     * the unselected-foreground color to the specified color.
     *
     * @param c set the foreground color to this value
     */
    public void setForeground(final Color c) {
        super.setForeground(c);
        unselectedForeground = c;
    }

    /**
     * Overrides <code>JComponent.setBackground</code> to assign
     * the unselected-background color to the specified color.
     *
     * @param c set the background color to this value
     */
    public void setBackground(final Color c) {
        super.setBackground(c);
        unselectedBackground = c;
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     */
    @Override public boolean isOpaque() {
        final Color back = getBackground();

        final Component p = getParent();
        final Component table = p != null ? p.getParent() : null;

        // table should now be the JTable.
        boolean colorMatch = (back != null) && (table != null) &&
                back.equals(table.getBackground()) && table.isOpaque();
        return !colorMatch && super.isOpaque();
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     */
    @Override public void validate() {
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     */
    @Override public void revalidate() {
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     */
    @Override public void repaint(long tm, int x, int y, int width, int height) {
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     */
    @Override public void repaint(Rectangle r) {
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     */
    @Override protected void firePropertyChange(String propertyName, Object oldValue,
                                      Object newValue) {
        if (propertyName.equals("text")) {
            super.firePropertyChange(propertyName, oldValue, newValue);
        }
    }
}
