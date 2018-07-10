package edu.gemini.qpt.ui.util;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.ScrollPaneConstants;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.JViewport;

public class ScrollPanes {

    /**
     * Sizes a JScrollPane's viewport's height in terms of multiples of the row height of its view,
     * and the width in terms of the view's preferred width. This operation only works if the view 
     * is a  JTable or JTree.
     * @param scroll JScrollPane whose viewport is to be sized
     * @param rows
     * @throws IllegalArgumentException if <code>scroll</code>'s view is not a JTable or JTree.
     */
    public static void setViewportHeight(JScrollPane scroll, int rows) {

        // The viewport and view.
        JViewport viewport = scroll.getViewport();
        Component view = viewport.getView();

        // Find the row height
        final int rowHeight;
        if (view instanceof JTable) {
            rowHeight = ((JTable) view).getRowHeight();
        } else if (view instanceof JTree) {
            rowHeight = ((JTree) view).getRowHeight();
        } else {
            throw new IllegalArgumentException("JScrollPane's view is not a JTable or JTree.");
        }
        
        // Size is the view's preferred width, and rowHeight * rows
        Dimension size = view.getPreferredSize();
        size.height = rowHeight * rows;
        
        // Done.
        viewport.setMinimumSize(size);
        viewport.setPreferredSize(size);
        
    }

    /**
     * Sets the viewport's width to be the same as its content width.
     * @param scroll
     */
    public static void setViewportWidth(JScrollPane scroll) {
    

        // The viewport and view.
        JViewport viewport = scroll.getViewport();
        Component view = viewport.getView();
        
        // Size is the view's preferred width, and viewport's default height
        Dimension size = viewport.getPreferredSize();
        size.width = view.getPreferredSize().width;

        // In order to display all of the contents properly, add room for the scroll bars
        // as the scroll bar policy dictates. The width of scroll bars are 0 until
        // they are displayed, so we must use their preferred sizes.
        if (scroll.getVerticalScrollBarPolicy() == ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            || scroll.getVerticalScrollBarPolicy() == ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS)
            size.width += scroll.getVerticalScrollBar().getPreferredSize().getWidth();

        // Done.
        viewport.setMinimumSize(size);
        viewport.setPreferredSize(size);
        

    }
    
}
