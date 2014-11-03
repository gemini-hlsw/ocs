package jsky.image.gui;


import edu.gemini.shared.util.immutable.Pair;
import jsky.image.ImageChangeEvent;
import jsky.util.I18N;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

public final class ImageScaleMenu extends JMenu {
    private static final I18N _I18N     = I18N.getInstance(ImageDisplayMenuBar.class);
    private static final int  MAX_SCALE = ImageDisplayMenuBar.MAX_SCALE;

    private final HashMap<Pair<Integer, Integer>, JRadioButtonMenuItem> scaleToButton;

    public ImageScaleMenu(final DivaMainImageDisplay imageDisplay) {
        super(_I18N.getString("scale"));

        // A lookup to be able to mark the appropriate radio button as selected when the image scale changes externally.
        scaleToButton = new HashMap<>();

        /** Create the zoom in and zoom out menu items **/
        final ButtonGroup group = new ButtonGroup();

        for (ScaleMenuOptions o : ScaleMenuOptions.values()) {
            final JMenu menu = new JMenu(o.i18nName);
            for (int i=o.lowerBound; i <= o.upperBound; ++i) {
                final float scale                     = o.createScaleForIndex(i);
                final Pair<Integer, Integer> rational = o.createRationalScaleForIndex(i);

                final JRadioButtonMenuItem b = new JRadioButtonMenuItem(o.createLabelForIndex(i));
                b.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        imageDisplay.setScale(scale);
                        imageDisplay.updateImage();
                    }
                });
                group.add(b);
                menu.add(b);
                scaleToButton.put(rational, b);
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
        final Pair<Integer, Integer> rational = convertScaleToRational(scale);
        final JRadioButtonMenuItem b          = scaleToButton.get(rational);
        if (b != null)
            b.setSelected(true);
    }


    private static Pair<Integer, Integer> convertScaleToRational(float scale) {
        if (scale > 0 && scale < 1)
            return new Pair<>(1, Math.round(1.0f/scale));
        return new Pair<>(Math.round(scale),1);
    }

    private enum ScaleMenuOptions {
        ZoomOut("zoomOut", 2, MAX_SCALE) {
            @Override
            String createLabelForIndex(int idx) {
                return "1/" + idx + "x";
            }

            @Override
            Pair<Integer, Integer> createRationalScaleForIndex(int idx) {
                return new Pair<>(1, idx);
            }
        },
        ZoomIn("zoomIn", 1, MAX_SCALE) {
            @Override
            String createLabelForIndex(int idx) {
                return idx + "x";
            }

            @Override
            Pair<Integer, Integer> createRationalScaleForIndex(int idx) {
                return new Pair<>(idx, 1);
            }
        },;

        // Name of this item.
        final String i18nName;
        final int    lowerBound;
        final int    upperBound;

        ScaleMenuOptions(final String name, final int lowerBound, final int upperBound) {
            i18nName        = _I18N.getString(name);
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
        }

        abstract String                 createLabelForIndex(int idx);
        abstract Pair<Integer, Integer> createRationalScaleForIndex(int idx);

        float createScaleForIndex(int idx) {
            final Pair<Integer, Integer> rational = createRationalScaleForIndex(idx);

            // This should never happen.
            if (rational._2() == 0)
                return 0;

            return ((float) rational._1()) / rational._2();
        }
    }
}
