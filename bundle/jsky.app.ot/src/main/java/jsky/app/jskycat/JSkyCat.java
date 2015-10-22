package jsky.app.jskycat;

import jsky.image.gui.MainImageDisplay;
import jsky.navigator.NavigatorImageDisplayFrame;
import jsky.util.I18N;
import jsky.util.gui.*;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * Main class for the JSkyCat application.
 */
public class JSkyCat extends JFrame {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(JSkyCat.class);

    /** The main image frame (or internal frame) */
    protected Component imageFrame;


    /**
     * Create the JSkyCat application class and display the contents of the
     * given image file or URL, if not null.
     *
     * @param imageFileOrUrl an image file or URL to display
     * @param portNum if not zero, listen on this port for remote control commnds
     *
     * @see JSkyCatRemoteControl
     */
    private JSkyCat(String imageFileOrUrl, int portNum) {
        super("JSky");

        makeFrameLayout(imageFileOrUrl);

        // Clean up on exit
        addWindowListener(new BasicWindowMonitor());

        if (portNum > 0) {
            try {
                new JSkyCatRemoteControl(portNum, this).start();
            } catch (IOException e) {
                DialogUtil.error(e);
            }
        }
    }

    /**
     * Create the JSkyCat application class and display the contents of the
     * given image file or URL, if not null.
     *
     * @param imageFileOrUrl an image file or URL to display
     */
    public JSkyCat(String imageFileOrUrl) {
        this(imageFileOrUrl, 0);
    }

    /**
     * Do the window layout using normal frames
     *
     * @param imageFileOrUrl an image file or URL to display
     */
    protected void makeFrameLayout(String imageFileOrUrl) {
        this.imageFrame = makeNavigatorImageDisplayFrame(imageFileOrUrl);
    }

    /** Return the name of this application. */
    protected String getAppName() {
        return "JSkyCat";
    }

    /** Return the version number of this application as a String. */
    protected String getAppVersion() {
        return JSkyCatVersion.JSKYCAT_VERSION.substring(5);
    }

    /**
     * Make and return a frame for displaying the given image (may be null).
     *
     * @param imageFileOrUrl specifies the iamge file or URL to display
     */
    protected NavigatorImageDisplayFrame makeNavigatorImageDisplayFrame(String imageFileOrUrl) {
        NavigatorImageDisplayFrame f = new NavigatorImageDisplayFrame(imageFileOrUrl);
        f.getImageDisplayControl().getImageDisplay().setTitle(getAppName() + " - version " + getAppVersion());
        f.setVisible(true);
        return f;
    }

    /**
     * Exit the application with the given status.
     */
    public void exit() {
        System.exit(0);
    }


    /** Return the main image frame (JFrame or JInternalFrame) */
    public Component getImageFrame() {
        return imageFrame;
    }

    /** Return the main image display */
    protected MainImageDisplay getImageDisplay() {
        if (imageFrame instanceof NavigatorImageDisplayFrame)
            return ((NavigatorImageDisplayFrame) imageFrame).getImageDisplayControl().getImageDisplay();
        return null;
    }

    /**
     * Convenience method to set the visibility of the image JFrame (or JInternalFrame).
     */
    public void setImageFrameVisible(boolean visible) {
        if (imageFrame != null) {
            imageFrame.setVisible(visible);

            if (visible)
                SwingUtil.showFrame(imageFrame);
        }
    }

}

