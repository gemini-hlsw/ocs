package jsky.coords.gui;

import jsky.coords.HMS;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Used to reformat RA,DEC coordinates in a JTable in sexagesimal notation
 * for display.
 */
public class SexagesimalTableCellRenderer extends DefaultTableCellRenderer {

    // Divide by this value to convert to deg to hours, if requested
    private float _f = 1.0F;

    // if true, display h:m:s, otherwise h:m
    private boolean _showSeconds = true;

    /** Constructor.
     *
     * @param hoursFlag if true, divide the cell value by 15 and display hours : min : sec,
     *                  otherwise display deg : min : sec.
     */
    public SexagesimalTableCellRenderer(boolean hoursFlag) {
        if (hoursFlag)
            _f = 15.0F;
        setHorizontalAlignment(JLabel.RIGHT);
    }

    /** Constructor.
     *
     * @param hoursFlag if true, divide the cell value by 15 and display hours : min : sec,
     *                  otherwise display deg : min : sec.
     * @param showSeconds if true, display h:m:s, otherwise h:m
     */
    public SexagesimalTableCellRenderer(boolean hoursFlag, boolean showSeconds) {
        this(hoursFlag);
        _showSeconds = showSeconds;
        setHorizontalAlignment(JLabel.RIGHT);
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        double val = Double.NaN;
        if (value != null) {
            if (value instanceof Float)
                val = ((Float) value).doubleValue();
            else if (value instanceof Double)
                val = ((Double) value);
        }
        if (!Double.isNaN(val)) {
            HMS hms = new HMS(val / _f);
            ((JLabel) component).setText(hms.toString(_showSeconds));
        }

        return component;
    }
}

