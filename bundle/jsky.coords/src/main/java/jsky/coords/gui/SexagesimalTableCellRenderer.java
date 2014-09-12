/*
 * ESO Archive
 *
 * $Id: SexagesimalTableCellRenderer.java 7983 2007-07-31 15:20:11Z swalker $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/07/23  Created
 */

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
        setHorizontalAlignment(JLabel.CENTER);
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
        setHorizontalAlignment(JLabel.CENTER);
    }


    /**
     * This method is sent to the renderer by the drawing table to
     * configure the renderer appropriately before drawing.  Return
     * the Component used for drawing.
     *
     * @param	table		the JTable that is asking the renderer to draw.
     *				This parameter can be null.
     * @param	value		the value of the cell to be rendered.  It is
     *				up to the specific renderer to interpret
     *				and draw the value.  eg. if value is the
     *				String "true", it could be rendered as a
     *				string or it could be rendered as a check
     *				box that is checked.  null is a valid value.
     * @param	isSelected	true is the cell is to be renderer with
     *				selection highlighting
     * @param	hasFocus	true if the cell has the focus
     * @param	row	        the row index of the cell being drawn.  When
     *				drawing the header the rowIndex is -1.
     * @param	column	        the column index of the cell being drawn
     */
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        double val = Double.NaN;
        if (value != null) {
            if (value instanceof Float)
                val = ((Float) value).doubleValue();
            else if (value instanceof Double)
                val = ((Double) value).doubleValue();
        }
        if (!Double.isNaN(val)) {
            HMS hms = new HMS(val / _f);
            ((JLabel) component).setText(hms.toString(_showSeconds));
        }

        return component;
    }
}

