package jsky.image.gui;

import java.awt.BorderLayout;

import javax.swing.JFrame;

import jsky.app.ot.util.Resources;
import jsky.util.I18N;
import jsky.util.Preferences;

/**
 * Provides a top level window for an ImageColors panel.
 *
 * @version $Revision: 4414 $
 * @author Allan Brighton
 */
public class ImageColorsFrame extends JFrame {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(ImageColorsFrame.class);

    // The GUI panel
    private ImageColors imageColors;


    /**
     * Create a top level window containing an ImageColors panel.
     */
    public ImageColorsFrame(BasicImageDisplay imageDisplay) {
        super(_I18N.getString("imageColors"));
        imageColors = new ImageColors(this, imageDisplay);
        getContentPane().add(imageColors, BorderLayout.CENTER);
        Resources.setOTFrameIcon(this);
        pack();
        Preferences.manageLocation(this);
        setVisible(true);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
    }

    /** Return the internal panel object */
    public ImageColors getImageColors() {
        return imageColors;
    }
}

