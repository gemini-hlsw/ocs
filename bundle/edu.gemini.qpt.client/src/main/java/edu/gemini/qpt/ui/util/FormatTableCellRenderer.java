package edu.gemini.qpt.ui.util;

import java.awt.Component;
import java.text.Format;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * TableCellRenderer that renders a Date with a user-specfied format.
 * @author rnorris
 */
@SuppressWarnings("serial")
public class FormatTableCellRenderer extends DefaultTableCellRenderer {

    private final Format format;
    
    public FormatTableCellRenderer(Format format) {
        this.format = format;
//        setHorizontalAlignment(SwingConstants.RIGHT);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean arg2, boolean arg3, int arg4, int arg5) {
        Component ret = super.getTableCellRendererComponent(table, value, arg2, arg3, arg4, arg5);
        if (value != null) setText(format.format(value));
        return ret;        
    }
    

}
