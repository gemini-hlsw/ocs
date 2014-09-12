/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: ImageGraphicsMenu.java 6013 2005-05-02 20:55:16Z brighton $
 */

package jsky.image.graphics.gui;


import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jsky.image.ImageChangeEvent;
import jsky.image.gui.DivaMainImageDisplay;
import jsky.util.I18N;
import jsky.util.gui.BasicWindowMonitor;


/**
 * A menu with graphics related items, for drawing and manipulating figures
 * on an image.
 *
 * @version $Revision: 6013 $
 * @author Allan Brighton
 */
public class ImageGraphicsMenu extends JMenu {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(ImageGraphicsMenu.class);

    /** Object managing the drawing */
    protected CanvasDraw canvasDraw;

    /** Array of menu items. */
    protected JRadioButtonMenuItem[] drawingModeMenuItems = new JRadioButtonMenuItem[CanvasDraw.NUM_DRAWING_MODES];

    /** "Save Graphics With Image" Menu item */
    protected JMenuItem saveGraphicsMenuItem;


    /** Create a menu with graphics related items */
    public ImageGraphicsMenu(CanvasDraw canvasDraw) {
        super("Graphics");
        this.canvasDraw = canvasDraw;
        add(createDrawingModeMenu());
        add(createLineWidthMenu());
        add(createOutlineMenu());
        add(createFillMenu());
        add(createCompositeMenu());
        add(createFontMenu());
        addSeparator();
        add(canvasDraw.deleteSelectedAction);
        add(canvasDraw.clearAction);
        addSeparator();
        add(new JCheckBoxMenuItem(canvasDraw.hideGraphicsAction));
        addSeparator();
        add(saveGraphicsMenuItem = createSaveGraphicsWithImageMenuItem());

        // register to receive notification when the image changes, to enable/disable some menu items
        canvasDraw.getImageDisplay().addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent ce) {
                ImageChangeEvent e = (ImageChangeEvent) ce;
                if (e.isNewImage() && !e.isBefore())
                    updateStates();
            }
        });
        updateStates();
    }


    /** Update the enabled states of some menu items */
    protected void updateStates() {
        saveGraphicsMenuItem.setEnabled(canvasDraw.getImageDisplay().getFitsImage() != null);
    }


    /** Create the "Drawing Mode" menu */
    protected JMenu createDrawingModeMenu() {
        JMenu menu = new JMenu(_I18N.getString("drawingMode"));
        ButtonGroup group = new ButtonGroup();

        for (int i = 0; i < CanvasDraw.NUM_DRAWING_MODES; i++)
            createDrawingModeMenuItem(i, menu, group);

        drawingModeMenuItems[0].setSelected(true);

        // arrange to select the menu item when the mode is changed
        canvasDraw.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                int drawingMode = canvasDraw.getDrawingMode();
                if (!drawingModeMenuItems[drawingMode].isSelected())
                    drawingModeMenuItems[drawingMode].setSelected(true);
            }
        });

        return menu;
    }

    /** Create the menu item for the given mode */
    protected void createDrawingModeMenuItem(int drawingMode, JMenu menu,
                                             ButtonGroup group) {
        AbstractAction a = canvasDraw.getDrawingModeAction(drawingMode);
        JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(a);
        menu.add(menuItem);
        group.add(menuItem);
        drawingModeMenuItems[drawingMode] = menuItem;
    }


    /** Create the "Line Width" menu */
    protected JMenu createLineWidthMenu() {
        JMenu menu = new JMenu(_I18N.getString("lineWidth"));
        ButtonGroup group = new ButtonGroup();
        int n = CanvasDraw.NUM_LINE_WIDTHS;

        for (int i = 0; i < n; i++) {
            JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(canvasDraw.getLineWidthAction(i));
            menu.add(menuItem);
            group.add(menuItem);
        }
        return menu;
    }

    /** Create the "Outline" menu */
    protected JMenu createOutlineMenu() {
        JMenu menu = new JMenu(_I18N.getString("outline"));
        ButtonGroup group = new ButtonGroup();
        int n = CanvasDraw.NUM_COLORS;
        for (int i = 0; i < n; i++) {
            JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(canvasDraw.getOutlineAction(i));
            menuItem.setBackground(CanvasDraw.COLORS[i]);
            menu.add(menuItem);
            group.add(menuItem);
        }
        return menu;
    }

    /** Create the "Fill" menu */
    protected JMenu createFillMenu() {
        JMenu menu = new JMenu(_I18N.getString("fill"));
        ButtonGroup group = new ButtonGroup();
        int n = CanvasDraw.NUM_COLORS;
        for (int i = 0; i < n; i++) {
            JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(canvasDraw.getFillAction(i));
            menuItem.setBackground(CanvasDraw.COLORS[i]);
            menu.add(menuItem);
            group.add(menuItem);
        }
        return menu;
    }

    /** Create the "Composite" menu */
    protected JMenu createCompositeMenu() {
        JMenu menu = new JMenu(_I18N.getString("composite"));
        ButtonGroup group = new ButtonGroup();
        int n = CanvasDraw.NUM_COMPOSITES;
        for (int i = 0; i < n; i++) {
            JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(canvasDraw.getCompositeAction(i));
            menu.add(menuItem);
            group.add(menuItem);
        }
        return menu;
    }

    /** Create the "Font" menu */
    protected JMenu createFontMenu() {
        JMenu menu = new JMenu(_I18N.getString("font"));
        ButtonGroup group = new ButtonGroup();
        int n = CanvasDraw.NUM_FONTS;
        for (int i = 0; i < n; i++) {
            JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(canvasDraw.getFontAction(i));
            menuItem.setFont(CanvasDraw.FONTS[i]);
            menu.add(menuItem);
            group.add(menuItem);
        }
        return menu;
    }

    /** Create and return the "Save Graphics With Image" menu item. */
    protected JMenuItem createSaveGraphicsWithImageMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("saveGraphicsWithImage"));
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                canvasDraw.imageDisplay.saveGraphicsWithImage();
            }
        });
        return menuItem;
    }


    /**
     * test main: usage: java GraphicsImageDisplay <filename>.
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame("ImageGraphicsMenu");
        DivaMainImageDisplay imageDisplay = new DivaMainImageDisplay();
        if (args.length > 0) {
            try {
                imageDisplay.setFilename(args[0]);
            } catch (Exception e) {
                System.out.println("error: " + e.toString());
                System.exit(1);
            }
        }

        CanvasDraw canvasDraw = new CanvasDraw(imageDisplay);
        JMenuBar menubar = new JMenuBar();
        menubar.add(new ImageGraphicsMenu(canvasDraw));

        frame.getContentPane().add(menubar, BorderLayout.NORTH);
        frame.getContentPane().add(imageDisplay, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new BasicWindowMonitor());
    }
}
