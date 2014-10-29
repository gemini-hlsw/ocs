package jsky.image.gui;

import jsky.util.I18N;
import jsky.util.Preferences;

import javax.media.jai.Interpolation;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public final class ImageScaleInterpolationMenu extends JMenu {
    private static final String PREF_KEY_INTERPOLATION = ImageDisplayMenuBar.class.getName() + ".ScaleInterpolation";
    private static final I18N _I18N                    = I18N.getInstance(ImageDisplayMenuBar.class);

    public ImageScaleInterpolationMenu(final DivaMainImageDisplay imageDisplay) {
        super(_I18N.getString("scaleInt"));
        final ButtonGroup group = new ButtonGroup();

        final ItemListener itemListener = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                final JRadioButtonMenuItem rb = (JRadioButtonMenuItem) e.getSource();
                if (rb.isSelected()) {
                    final ScaleInterpolation si = ScaleInterpolation.getScaleInterpolationForButton(rb);
                    si.storeAsPreference();

                    imageDisplay.setInterpolation(Interpolation.getInstance(si.interpolation));
                    imageDisplay.updateImage();
                }
            }
        };

        for (ScaleInterpolation si : ScaleInterpolation.values()) {
            final JRadioButtonMenuItem menuItem = si.menuItem;
            add(menuItem);
            group.add(menuItem);
            menuItem.addItemListener(itemListener);
        }
        ScaleInterpolation.loadPreference();
    }

    private enum ScaleInterpolation {
        Nearest (Interpolation.INTERP_NEAREST),
        Bilinear(Interpolation.INTERP_BILINEAR),
        Bicubic (Interpolation.INTERP_BICUBIC),
        Bicubic2(Interpolation.INTERP_BICUBIC_2),
        ;

        final int                  interpolation;
        final JRadioButtonMenuItem menuItem;

        ScaleInterpolation(int interpolation) {
            this.interpolation = interpolation;
            menuItem           = new JRadioButtonMenuItem(name());
        }

        void storeAsPreference() {
            Preferences.set(PREF_KEY_INTERPOLATION, name());
        }

        static void loadPreference() {
            final String pref = Preferences.get(PREF_KEY_INTERPOLATION, Nearest.name());
            try {
                ScaleInterpolation.valueOf(pref).menuItem.setSelected(true);
            } catch (Exception e) {
                e.printStackTrace();
                Nearest.menuItem.setSelected(true);
            }
        }

        static ScaleInterpolation getScaleInterpolationForButton(final JRadioButtonMenuItem button) {
            try {
                return ScaleInterpolation.valueOf(button.getText());
            } catch (Exception e) {
                return Nearest;
            }
        }
    }
}
