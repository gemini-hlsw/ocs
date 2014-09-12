/**
 * $Id: JTextAreaCellRenderer.java 5903 2005-03-17 12:39:48Z brighton $
 */

package jsky.util.gui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;


/**
 * A table cell renderer for multi-line text fields.
 */
public class JTextAreaCellRenderer extends JTextArea implements TableCellRenderer {

    private static final int ROW_PAD = 3;

    protected static Border noFocusBorder = new EmptyBorder(3, 3, 3, 3);
    protected static Border focusBorder = new CompoundBorder(
            UIManager.getBorder("Table.focusCellHighlightBorder"),
            new EmptyBorder(2, 2, 2, 2));

    private Color unselectedForeground;
    private Color unselectedBackground;

    public JTextAreaCellRenderer() {
        setWrapStyleWord(true);
        setLineWrap(true);
        setOpaque(true);
        setBorder(noFocusBorder);
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
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
//            setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
            setBorder(focusBorder);
            if (table.isCellEditable(row, column)) {
                super.setForeground(UIManager.getColor("Table.focusCellForeground"));
                super.setBackground(UIManager.getColor("Table.focusCellBackground"));
            }
        } else {
            setBorder(noFocusBorder);
        }

        setText((value == null) ? "" : value.toString());
        _updateRowHeight(row, column, table);

        return this;
    }

    private void _updateRowHeight(int row, int column, JTable table) {
        if (row < 0 || column < 0) {
            return;
        }

        int tableColWidth = table.getColumnModel().getColumn(column).getWidth();
        Dimension d = getPreferredSize();
        if (d.width != tableColWidth) {
            setSize(new Dimension(tableColWidth, 1000));
            d = getPreferredSize();
        }

        int tableRowHeight = table.getRowHeight(row);
        int defaultTableRowHeight = table.getRowHeight();
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
    public void setForeground(Color c) {
        super.setForeground(c);
        unselectedForeground = c;
    }

    /**
     * Overrides <code>JComponent.setBackground</code> to assign
     * the unselected-background color to the specified color.
     *
     * @param c set the background color to this value
     */
    public void setBackground(Color c) {
        super.setBackground(c);
        unselectedBackground = c;
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     */
    public boolean isOpaque() {
        Color back = getBackground();
        Component p = getParent();
        if (p != null) {
            p = p.getParent();
        }
        // p should now be the JTable.
        boolean colorMatch = (back != null) && (p != null) &&
                back.equals(p.getBackground()) && p.isOpaque();
        return !colorMatch && super.isOpaque();
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     */
    public void validate() {
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     */
    public void revalidate() {
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     */
    public void repaint(long tm, int x, int y, int width, int height) {
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     */
    public void repaint(Rectangle r) {
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     */
    protected void firePropertyChange(String propertyName, Object oldValue,
                                      Object newValue) {
        // Strings get interned...
        if (propertyName == "text") {
            super.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     */
//    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) { }

    /**
     * A subclass of <code>DefaultTableCellRenderer</code> that
     * implements <code>UIResource</code>.
     * <code>DefaultTableCellRenderer</code> doesn't implement
     * <code>UIResource</code>
     * directly so that applications can safely override the
     * <code>cellRenderer</code> property with
     * <code>DefaultTableCellRenderer</code> subclasses.
     * <p/>
     * <strong>Warning:</strong>
     * Serialized objects of this class will not be compatible with
     * future Swing releases. The current serialization support is
     * appropriate for short term storage or RMI between applications running
     * the same version of Swing.  As of 1.4, support for long term storage
     * of all JavaBeans<sup><font size="-2">TM</font></sup>
     * has been added to the <code>java.beans</code> package.
     * Please see {@link java.beans.XMLEncoder}.
     */
    public static class UIResource extends DefaultTableCellRenderer
            implements javax.swing.plaf.UIResource {

    }
}