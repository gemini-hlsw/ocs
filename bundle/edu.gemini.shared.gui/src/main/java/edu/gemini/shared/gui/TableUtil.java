// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: TableUtil.java 6545 2005-08-16 13:17:04Z shane $
//

package edu.gemini.shared.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Enumeration;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.table.*;

/**
 * A utility class to simplify some common JTable needs.
 *
 * @author	Shane Walker
 */
public final class TableUtil {

    /**
     * Scrolls the JTable to make the given row visible.
     *
     * @param jTable the table to scroll
     * @param row the row that should be made visible
     *
     * @see #scrollToRow(JTable, JViewport, int)
     */
    public static void scrollToRow(JTable jTable, int row) {
        // See if the table is immediately contained in a JViewport.  If so,
        // use it to make sure that the horizontal positon of the table doesn't
        // change.

        java.awt.Component parent = jTable.getParent();
        if (parent instanceof JViewport) {
            scrollToRow(jTable, (JViewport) parent, row);
        } else {
            scrollToRow(jTable, null, row);
        }
    }

    /**
     * Scrolls the JTable to make the given row visible.  Keeps the horizontal
     * position of the table the same, provided that the <code>viewPort</code>
     * argument references the JViewport in which the table is visible.
     *
     * @param jTable the table to scroll
     * @param viewPort the viewport that the table is displayed in (used to get
     * the x coordinate of the leftmost portion of the table that is visible)
     * @param row the row that should be made visible
     *
     * @see #scrollToRow(JTable, JViewport, int)
     */
    public static void scrollToRow(JTable jTable, JViewport viewPort, int row) {
        int x = 0;
        if (viewPort != null) {
            Rectangle viewRect = viewPort.getViewRect();
            x = viewRect.x;
        }
        int rh = jTable.getRowHeight() + jTable.getRowMargin();
        int y = rh * row;
        Rectangle rect = new Rectangle(x, y, 0, rh);
        jTable.scrollRectToVisible(rect);

        //   boolean scroll = true;
        //   if (viewPort != null) {
        //      Rectangle viewRect = viewPort.getViewRect();
        //
        //      // Doesn't exist in JDK1.1, so have to implement here.
        //      //scroll = !viewRect.contains(rect);
        //      //scroll = !viewRect.contains(rect.x, rect.y);
        //      System.out.println("viewRect = " + viewRect);
        //      System.out.println("rect     = " + rect);
        //      scroll = !(viewRect.contains(rect.x, rect.y) &&
        //                 viewRect.contains(rect.x + rect.width, rect.y + rect.height));
        //      System.out.println("scroll = " + scroll);
        //   }
        //
        //   if (scroll) {
        //      jTable.scrollRectToVisible(rect);
        //   }
    }

    /**
     * Scrolls the JTable to make the given cell visible.
     *
     * @param jTable the table to scroll
     * @param row the row of the cell that should be made visible
     * @param column the column of the cell that should be made visible
     */
    public static void scrollToCell(JTable jTable, int row, int column) {
        TableColumnModel tcm = jTable.getColumnModel();
        int margin = tcm.getColumnMargin();
        int x = 0;
        for (int i = 0; i < column; ++i) {
            TableColumn tc = tcm.getColumn(i);
            x += tc.getWidth() + margin;
        }
        int cw = tcm.getColumn(column).getWidth();
        int rh = jTable.getRowHeight() + jTable.getRowMargin();
        int y = rh * row;
        jTable.scrollRectToVisible(new Rectangle(x, y, cw, rh));
    }

    /**
     * Resize the table's columns to fit their content.  This method is only
     * appropriate for tables that display on string content and contain no
     * custom renderers.  For tables that contain custom renderers for one or
     * more cells, use {@link #minimizeColumnWidths}.
     */
    public static void resizeColumnsToFitContent(JTable jTable) {
        TableModel tm = jTable.getModel();
        //jTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );

        int cc = tm.getColumnCount();
        int[] colWidth = new int[cc];
        String[] colMaxStr = new String[cc];

        // Find the longest string, in characters, in each column.
        // Initialize with the column names themselves, then look at each row.
        for (int i = 0; i < cc; ++i) {
            String s = tm.getColumnName(i);
            colWidth[i] = s.length();
            colMaxStr[i] = s;
        }
        int rc = tm.getRowCount();
        for (int r = 0; r < rc; ++r) {
            for (int c = 0; c < cc; ++c) {
                // String s = tm.getValueAt(r, c).toString();
                Object value = tm.getValueAt(r, c);
                if (value == null)
                    continue;
                String s = value.toString();
                if (s.length() > colWidth[c]) {
                    colWidth[c] = s.length();
                    colMaxStr[c] = s;
                }
            }
        }

        // Get the FontMetrics
        FontMetrics fm = jTable.getFontMetrics(jTable.getFont());
        int maxAdvance = fm.getMaxAdvance();

        // Resize the table columns.  Obviously, the longest string in
        // characters isn't necessarily the widest string but it should
        // be close enough.
        for (int i = 0; i < cc; ++i) {
            TableColumn tc = jTable.getColumn(tm.getColumnName(i));
            //int size = maxAdvance * colWidth[i];
            int size = fm.stringWidth(colMaxStr[i] + maxAdvance);
            //tc.setWidth( size );
            tc.setPreferredWidth(size);
        }
    }

    /**
     * Resets a table's column's widths to be the minimum necessary size to
     * display their content.  (Taken from David Geary's Graphic Java.)
     * This method relies upon each column having a renderer that acurately
     * reports the column's preferred width.
     */
    public static void minimizeColumnWidths(JTable table) {
        Enumeration enm = table.getColumnModel().getColumns();
        while (enm.hasMoreElements()) {
            TableColumn col = (TableColumn) enm.nextElement();
            int prefWidth = getPreferredWidthForColumn(table, col);
            col.setMinWidth(prefWidth);
            col.setPreferredWidth(prefWidth);
            //col.setMaxWidth(prefWidth);
        }
    }

    /**
     * Calculates the preferred width for the column based upon the actual
     * content of the column. (Taken from David Geary's Graphic Java.)
     */
    public static int getPreferredWidthForColumn(JTable table, TableColumn col) {
        int hw = getColumnHeaderWidth(table, col);
        int cw = getWidestCellWidth(table, col);
        return hw > cw ? hw : cw;
    }

    public static int getColumnHeaderWidth(JTable table, TableColumn col) {
        TableCellRenderer rend = col.getHeaderRenderer();
        if (rend == null)
            rend = table.getTableHeader().getDefaultRenderer();
        Component comp = rend.getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, 0);
        return comp.getPreferredSize().width;
    }

    public static int getWidestCellWidth(JTable table, TableColumn col) {
        int c = col.getModelIndex(), width = 0, maxw = 0;
        for (int r = 0; r < table.getRowCount(); ++r) {
            TableCellRenderer rend = table.getCellRenderer(r, c);
            Component comp = rend.getTableCellRendererComponent(table, table.getValueAt(r, c), false, false, r, c);
            width = comp.getPreferredSize().width;
            maxw = width > maxw ? width : maxw;
        }
        return maxw;
    }

    /**
     * Get a simple dialog box for displaying a JTable.
     */
    public static JDialog getDialog(JTable jTable, Icon icon, String message, String title) {
        JScrollPane jScroll = new JScrollPane(jTable);
        int rh = jTable.getRowHeight();
        int w = jTable.getPreferredSize().width;
        jScroll.setPreferredSize(new Dimension(w + 3, rh * 6 + 50));

        // Sadly, this doesn't work correctly ...  It positions the table at the
        // bottom of the dialog box, cropped with a big empty gap at the top.
        //Object[]    optMsg  = new Object[] { message, jScroll };
        //JOptionPane jOpt    = new JOptionPane(optMsg, JOptionPane.WARNING_MESSAGE);
        //JDialog jd = jOpt.createDialog(null, title);

        GridBagConstraints gbc;

        // So go through the pain of creating and initializing a dialog to show
        // everything.
        JPanel jpan = new JPanel(new GridBagLayout());
        jpan.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Place the icon on the left of everything, centered vertically.
        if (icon != null) {
            gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridheight = 2;
            gbc.insets = new Insets(0, 0, 5, 5);
            gbc.weighty = 100.0;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.VERTICAL;
            jpan.add(new JLabel(icon), gbc);
        }

        // Place the message along the top, starting at the left edge
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 5, 5, 0);
        gbc.weightx = 100.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        jpan.add(new JLabel(message), gbc);

        // Place the table in the center
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.insets = new Insets(5, 5, 5, 0);
        gbc.weightx = 100.0;
        gbc.weighty = 100.0;
        gbc.fill = GridBagConstraints.BOTH;
        jpan.add(jScroll, gbc);

        // Place the "ok" button along the bottom
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.insets = new Insets(5, 5, 0, 0);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        JButton okButton = new JButton("Ok");
        jpan.add(okButton, gbc);


        // Create the dialog (non-modal because there are too many problems
        // with modal dialogs).  Too bad I can use the "recycled" dialog box,
        // it is not visible outside of the swing package.

        final JDialog jd = new JDialog((Frame) null, title);
        jd.setContentPane(jpan);
        jd.pack();
        jd.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                jd.dispose();
            }
        });
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                jd.dispose();
            }
        });
        jd.setLocationRelativeTo(null);   // Center the dialog on the window
        return jd;
    }

}

