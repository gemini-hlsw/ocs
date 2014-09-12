//
// $
//

package edu.gemini.shared.gui;

import javax.swing.*;
import java.awt.image.*;
import java.awt.*;

/**
 * A flat button is a simple JButton extension with a few properties set to
 * give the button a flat appearance.  They are meant to be used with an
 * icon and rollover icon to give the user feedback that tells him that he
 * is working with a button.
 */
public final class ButtonFlattener {
    private ButtonFlattener() {
        // defeat instantiation
    }

    /**
     * Configures the given button to be displayed "flat" without decorations
     * such as a background or border.  It is expected that the button contain
     * an ImageIcon, which will be used to setup rollover and pressed button
     * images.
     *
     * @param btn button to configure as a flat button
     */
    public static void flatten(AbstractButton btn) {
        Icon icon = btn.getIcon();

        ImageIcon rollover = null;
        ImageIcon pressed  = null;
        if (icon instanceof ImageIcon) {
            rollover = getDarkerIcon((ImageIcon) icon);
            pressed  = getDarkerIcon(rollover);
        }

        flatten(btn, null, rollover, pressed);
    }

    /**
     * Creates with explicitly provided icons.
     */
    public static void flatten(AbstractButton btn, Icon normal, Icon rollover, Icon pressed) {
        if (normal != null) btn.setIcon(normal);
        if (rollover != null) btn.setRolloverIcon(rollover);
        if (pressed != null) btn.setPressedIcon(pressed);
        btn.setFocusable(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
    }

    private static ImageIcon getDarkerIcon(ImageIcon normal) {
        Image image = normal.getImage();
        ImageFilter filter     = HsbImageFilter.createImageIconDarkener();
        ImageProducer source   = image.getSource();
        ImageProducer producer = new FilteredImageSource(source, filter);
        Image darkerImage = Toolkit.getDefaultToolkit().createImage(producer);
        return new ImageIcon(darkerImage);
    }
}
