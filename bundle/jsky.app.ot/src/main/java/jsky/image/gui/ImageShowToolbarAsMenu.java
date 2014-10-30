package jsky.image.gui;

import jsky.util.I18N;
import jsky.util.Preferences;
import jsky.util.gui.GenericToolBar;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.logging.Logger;

public final class ImageShowToolbarAsMenu extends JMenu {
    private static final Logger LOGGER                 = Logger.getLogger(ImageShowToolbarAsMenu.class.getName());
    private static final String PREF_KEY_SHOWTOOLBARAS = ImageDisplayMenuBar.class.getName() + ".ShowToolBarAs";
    private static final I18N _I18N                    = I18N.getInstance(ImageDisplayMenuBar.class);

    public ImageShowToolbarAsMenu(final GenericToolBar toolBar) {
        super(_I18N.getString("showToolBarAs"));
        final ButtonGroup group = new ButtonGroup();

        final ItemListener itemListener = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                final JRadioButtonMenuItem rb = (JRadioButtonMenuItem) e.getSource();
                if (rb.isSelected()) {
                    final ToolBarOption tbo = ToolBarOption.getToolBarOptionForButton(rb);
                    tbo.storeAsPreference();

                    toolBar.setShowPictures(tbo.showPictures);
                    toolBar.setShowText(tbo.showText);
                }
            }
        };

        for (ToolBarOption tbo : ToolBarOption.values()) {
            final JRadioButtonMenuItem menuItem = tbo.menuItem;
            add(menuItem);
            group.add(menuItem);
            menuItem.addItemListener(itemListener);
        }
        ToolBarOption.loadPreference().menuItem.setSelected(true);
    }

    /**
     * For backwards compatibility, we must not only consider text strings representing the
     * preference option, but also the integers:
     * 1 -> picAndText
     * 2 -> picOnly
     * 3 -> textOnly
     */
    private enum ToolBarOption {
        picAndText(true,  true),
        picOnly   (true,  false),
        textOnly  (false, true),
        ;

        final boolean              showPictures;
        final boolean              showText;
        final JRadioButtonMenuItem menuItem;

        ToolBarOption(final boolean showPictures, final boolean showText) {
            this.showPictures = showPictures;
            this.showText     = showText;
            menuItem          = new JRadioButtonMenuItem(_I18N.getString(name()));
        }

        void storeAsPreference() {
            Preferences.set(PREF_KEY_SHOWTOOLBARAS, name());
        }

        static ToolBarOption loadPreference() {
            final String pref = Preferences.get(PREF_KEY_SHOWTOOLBARAS, picAndText.name());
            for (ToolBarOption tbo : ToolBarOption.values())
                if (pref.equals(tbo.name()) || pref.equals(String.valueOf(tbo.ordinal() + 1)))
                    return tbo;

            LOGGER.info("Illegal preference found for key " + PREF_KEY_SHOWTOOLBARAS + ": \"" + pref + "\". " +
                        "Using picAndText instead.");
            return picAndText;
        }

        static ToolBarOption getToolBarOptionForButton(final JRadioButtonMenuItem menuItem) {
            for (ToolBarOption tbo : ToolBarOption.values())
                if (menuItem.equals(tbo.menuItem))
                    return tbo;
            return picAndText;
        }
    }
}
