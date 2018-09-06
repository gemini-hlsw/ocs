package jsky.image.gui;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JFrame;

import jsky.app.ot.tpe.TpeImageWidget;
import jsky.util.gui.Resources;
import jsky.util.I18N;
import jsky.util.Preferences;

/**
 * Provides a top level window for an ImageDisplayControl panel.
 *
 * @version $Revision: 5923 $
 * @author Allan Brighton
 */
public abstract class ImageDisplayControlFrame extends JFrame {

    // Used to access internationalized strings (see i18n/gui*.properties)
    private static final I18N _I18N = I18N.getInstance(ImageDisplayControlFrame.class);

    /** The frame's toolbar */
    private ImageDisplayToolBar toolBar;

    /** Panel containing image display and controls */
    private ImageDisplayControl imageDisplayControl;

    /** Count of instances of thiss class */
    private static int openFrameCount = 0;

    /**
     * Create a top level window containing an ImageDisplayControl panel.
     *
     * @param size   the size (width, height) to use for the pan and zoom windows.
     */
    private ImageDisplayControlFrame(int size) {
        super(_I18N.getString("imageDisplay"));

        imageDisplayControl = makeImageDisplayControl(size);
        final TpeImageWidget imageWidget = imageDisplayControl.getImageDisplay();
        toolBar = makeToolBar(imageWidget);
        setJMenuBar(makeMenuBar(imageWidget, toolBar));

        Container contentPane = getContentPane();
        contentPane.add(toolBar, BorderLayout.NORTH);
        contentPane.add(imageDisplayControl, BorderLayout.CENTER);

        imageDisplayControl.setBorder(BorderFactory.createEtchedBorder());

        // set default window size and remember changes between sessions
        Preferences.manageSize(imageDisplayControl, new Dimension(650, 700));
        Preferences.manageLocation(this);
        openFrameCount++;

        Resources.setOTFrameIcon(this);
        pack();
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            @Override public void windowClosing(WindowEvent e) {
                imageWidget.close();
            }

            @Override public void windowClosed(WindowEvent e) {
                if (--openFrameCount == 0 && imageWidget.isMainWindow())
                    imageWidget.exit();
            }
        });
    }

    /**
     * Create a top level window containing an ImageDisplayControl panel.
     *
     * @param size   the size (width, height) to use for the pan and zoom windows.
     * @param fileOrUrl The file name or URL of an image to display.
     */
    private ImageDisplayControlFrame(int size, String fileOrUrl) {
        this(size);

        if (fileOrUrl != null) {
            imageDisplayControl.getImageDisplay().setFilename(fileOrUrl, true);
        } else {
            imageDisplayControl.getImageDisplay().blankImage(0., 0.);
        }
    }

    /**
     * Create a top level window containing an ImageDisplayControl panel.
     *
     * @param fileOrUrl The file name or URL of an image to display.
     */
    public ImageDisplayControlFrame(String fileOrUrl) {
        this(ImagePanner.DEFAULT_SIZE, fileOrUrl);
    }


    /** Return the internal ImageDisplayControl panel */
    public ImageDisplayControl getImageDisplayControl() {
        return imageDisplayControl;
    }


    /** Make and return the toolbar */
    protected ImageDisplayToolBar makeToolBar(TpeImageWidget mainImageDisplay) {
        return new ImageDisplayToolBar(mainImageDisplay);
    }

    /** Make and return the menubar */
    protected ImageDisplayMenuBar makeMenuBar(DivaMainImageDisplay mainImageDisplay, ImageDisplayToolBar toolBar) {
        return new ImageDisplayMenuBar(mainImageDisplay, toolBar);
    }

    /**
     * Make and return the image display control frame.
     *
     * @param size the size (width, height) to use for the pan and zoom windows.
     */
    protected abstract ImageDisplayControl makeImageDisplayControl(int size);

}
