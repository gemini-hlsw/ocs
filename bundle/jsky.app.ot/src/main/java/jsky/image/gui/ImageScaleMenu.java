package jsky.image.gui;


import jsky.image.ImageChangeEvent;
import jsky.util.I18N;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

public final class ImageScaleMenu extends JMenu {
    private static final I18N _I18N = I18N.getInstance(ImageDisplayMenuBar.class);
    private final HashMap<Float, JRadioButtonMenuItem> scaleToButton;

    public ImageScaleMenu(final DivaMainImageDisplay imageDisplay) {
        super(_I18N.getString("scale"));

        // A lookup to be able to mark the appropriate radio button as selected when the image scale changes externally.
        scaleToButton = new HashMap<>();

        /** Create the zoom in and zoom out menu items **/
        final ButtonGroup group = new ButtonGroup();
        final ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final float scale = Float.parseFloat(e.getActionCommand());
                imageDisplay.setScale(scale);
                imageDisplay.updateImage();
            }
        };

        for (ScaleMenuOptions o : ScaleMenuOptions.values()) {
            final JMenu menu = new JMenu(o.i18name);
            for (int i=1; i <= ImageDisplayMenuBar.MAX_SCALE; ++i) {
                final float scale = o.createScaleForIndex(i);

                final JRadioButtonMenuItem b = new JRadioButtonMenuItem(o.createLabelForIndex(i));
                b.setActionCommand(Float.toString(scale));
                b.addActionListener(listener);
                group.add(b);
                menu.add(b);
                scaleToButton.put(scale, b);
            }
            add(menu);
        }

        /** Create the fit to window menu item **/
        final JMenuItem menuItem = new JMenuItem(_I18N.getString("fitImageInWindow"));

        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                imageDisplay.scaleToFit();
                imageDisplay.updateImage();
            }
        });
        add(menuItem);

        /** Register a change listener to set the button if the scaling is changed externally. **/
        imageDisplay.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent ce) {
                final ImageChangeEvent e = (ImageChangeEvent) ce;
                if (e.isNewScale())
                    enableButtonForScale(imageDisplay.getScale());
            }
        });

        // Set the default button to selected.
        enableButtonForScale(imageDisplay.getScale());
    }

    private void enableButtonForScale(float scale) {
        final JRadioButtonMenuItem b = scaleToButton.get(scale);
        if (b != null)
            b.setSelected(true);
    }

    private enum ScaleMenuOptions {
        ZoomOut("zoomOut") {
            @Override
            String createLabelForIndex(int idx) {
                return "1/" + idx + "x";
            }

            @Override
            float createScaleForIndex(int idx) {
                return 1.0f / idx;
            }
        },
        ZoomIn("zoomIn") {
            @Override
            String createLabelForIndex(int idx) {
                return idx + "x";
            }

            @Override
            float createScaleForIndex(int idx) {
                return (float) idx;
            }
        },;

        // Name of this item.
        final String i18name;

        ScaleMenuOptions(final String name) {
            i18name = _I18N.getString(name);
        }

        abstract String createLabelForIndex(int idx);
        abstract float  createScaleForIndex(int idx);
    }
}
