package jsky.app.jskycat;

import jsky.image.gui.MainImageDisplay;
import jsky.navigator.NavigatorFrame;
import jsky.navigator.NavigatorImageDisplayFrame;
import jsky.navigator.NavigatorImageDisplayInternalFrame;
import jsky.navigator.NavigatorInternalFrame;
import jsky.util.I18N;
import jsky.util.Preferences;
import jsky.util.gui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

/**
 * Main class for the JSkyCat application.
 */
public class JSkyCat extends JFrame {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(JSkyCat.class);

    /** File selection dialog, when using internal frames */
    protected JFileChooser fileChooser;

    /** Main window, when using internal frames */
    @Deprecated // Not used in the OT
    protected static JDesktopPane desktop;

    /** The main image frame (or internal frame) */
    protected Component imageFrame;


    /**
     * Create the JSkyCat application class and display the contents of the
     * given image file or URL, if not null.
     *
     * @param imageFileOrUrl an image file or URL to display
     * @param internalFrames if true, use internal frames
     * @param portNum if not zero, listen on this port for remote control commnds
     *
     * @see JSkyCatRemoteControl
     */
    private JSkyCat(String imageFileOrUrl, boolean internalFrames, int portNum) {
        super("JSky");

        if (internalFrames || desktop != null) {
            makeInternalFrameLayout(imageFileOrUrl);
        } else {
            makeFrameLayout(imageFileOrUrl);
        }

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
        this(imageFileOrUrl, false, 0);
    }

    /** Return the JDesktopPane, if using internal frames, otherwise null */
    public static JDesktopPane getDesktop() {
        return desktop;
    }

    /** Set the JDesktopPane to use for top level windows, if using internal frames */
    public static void setDesktop(JDesktopPane dt) {
        desktop = dt;
    }

    /**
     * Do the window layout using internal frames
     *
     * @param imageFileOrUrl an image file or URL to display
     */
    @Deprecated //Not in use
    protected void makeInternalFrameLayout(String imageFileOrUrl) {
        boolean ownDesktop = false;   // true if this class owns the desktop
        if (desktop == null) {
            setJMenuBar(makeMenuBar());

            desktop = new JDesktopPane();
            desktop.setBorder(BorderFactory.createEtchedBorder());
            DialogUtil.setDesktop(desktop);
            ownDesktop = true;

            //Make dragging faster:
            desktop.putClientProperty("JDesktopPane.dragMode", "outline");

            // fill the whole screen
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            int w = screen.width - 10,
                    h = screen.height - 10;
            Preferences.manageSize(desktop, new Dimension(w, h), getClass().getName() + ".size");
            Preferences.manageLocation(this, 0, 0);

            setContentPane(desktop);
        }

        NavigatorImageDisplayInternalFrame imageFrame = makeNavigatorImageDisplayInternalFrame(desktop, imageFileOrUrl);
            this.imageFrame = imageFrame;
            desktop.add(imageFrame, JLayeredPane.DEFAULT_LAYER);
            desktop.moveToFront(imageFrame);
            imageFrame.setVisible(true);

        /*
        if (showNavigator) {
            if (imageFrame != null) {
                MainImageDisplay imageDisplay = imageFrame.getImageDisplayControl().getImageDisplay();
                navigatorFrame = makeNavigatorInternalFrame(desktop, imageDisplay);
                Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                int x = Math.min(screen.width - imageFrame.getWidth(), navigatorFrame.getWidth());
                imageFrame.setLocation(x, 0);
                imageFrame.setNavigator(navigatorFrame.getNavigator());
            } else {
                navigatorFrame = makeNavigatorInternalFrame(desktop, null);
            }
            desktop.add(navigatorFrame, JLayeredPane.DEFAULT_LAYER);
            desktop.moveToFront(navigatorFrame);
            navigatorFrame.setLocation(0, 0);
            navigatorFrame.setVisible(true);
        }*/

        if (ownDesktop) {
            pack();
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            addWindowListener(new WindowAdapter() {

                public void windowClosing(WindowEvent e) {
                    exit();
                }
            });
            setVisible(true);
        }
        setTitle(getAppName() + " - version " + getAppVersion());
    }

    /**
     * Do the window layout using normal frames
     *
     * @param imageFileOrUrl an image file or URL to display
     */
    protected void makeFrameLayout(String imageFileOrUrl) {
        //NavigatorFrame navigatorFrame;
        this.imageFrame = makeNavigatorImageDisplayFrame(imageFileOrUrl);

        /*if (showNavigator) {
            if (imageFrame != null) {
                MainImageDisplay imageDisplay = imageFrame.getImageDisplayControl().getImageDisplay();
                navigatorFrame = makeNavigatorFrame(imageDisplay);
                Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                int x = Math.min(screen.width - imageFrame.getWidth(), navigatorFrame.getWidth());
                imageFrame.setLocation(x, 0);
                imageFrame.setNavigator(navigatorFrame.getNavigator());
            } else {
                navigatorFrame = makeNavigatorFrame(null);
            }
            navigatorFrame.setLocation(0, 0);
            navigatorFrame.setVisible(true);
        }*/
    }

    /** Make and return the application menubar (used when internal frames are in use) */
    protected JMenuBar makeMenuBar() {
        return new JSkyCatMenuBar(this);
    }


    /**
     * Make and return an internal frame for displaying the given image (may be null).
     *
     * @param desktop used to display the internal frame
     * @param imageFileOrUrl specifies the iamge file or URL to display
     */
    private NavigatorImageDisplayInternalFrame makeNavigatorImageDisplayInternalFrame(JDesktopPane desktop, String imageFileOrUrl) {
        NavigatorImageDisplayInternalFrame f = new NavigatorImageDisplayInternalFrame(desktop, imageFileOrUrl);
        f.getImageDisplayControl().getImageDisplay().setTitle(getAppName());
        return f;
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
     * Create and return a new file chooser to be used to select a local catalog file
     * to open.
     */
    public JFileChooser makeFileChooser() {
        JFileChooser fileChooser = new JFileChooser(new File("."));

        ExampleFileFilter configFileFilter = new ExampleFileFilter(new String[]{"cfg"},
                                                                   _I18N.getString("catalogConfigFilesSkycat"));
        fileChooser.addChoosableFileFilter(configFileFilter);

        ExampleFileFilter skycatLocalCatalogFilter = new ExampleFileFilter(new String[]{"table", "tbl", "cat"},
                                                                           _I18N.getString("localCatalogFilesSkycat"));
        fileChooser.addChoosableFileFilter(skycatLocalCatalogFilter);

        ExampleFileFilter fitsFilter = new ExampleFileFilter(new String[]{"fit", "fits", "fts"},
                                                             _I18N.getString("fitsFileWithTableExt"));
        fileChooser.addChoosableFileFilter(fitsFilter);

        fileChooser.setFileFilter(fitsFilter);

        return fileChooser;
    }


    /**
     * Display a file chooser to select a filename to display in a new internal frame.
     */
    public void open() {
        if (fileChooser == null) {
            fileChooser = makeFileChooser();
        }
        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION && fileChooser.getSelectedFile() != null) {
            open(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }


    /**
     * Display the given file or URL in a new internal frame.
     */
    public void open(String fileOrUrl) {
        if (desktop != null) {
            if (fileOrUrl.endsWith(".fits") || fileOrUrl.endsWith(".fts")) {
                NavigatorImageDisplayInternalFrame frame = new NavigatorImageDisplayInternalFrame(desktop);
                desktop.add(frame, JLayeredPane.DEFAULT_LAYER);
                desktop.moveToFront(frame);
                frame.setVisible(true);
                frame.getImageDisplayControl().getImageDisplay().setFilename(fileOrUrl);
            } else {
                NavigatorInternalFrame frame = new NavigatorInternalFrame(desktop);
                frame.getNavigator().open(fileOrUrl);
                desktop.add(frame, JLayeredPane.DEFAULT_LAYER);
                desktop.moveToFront(frame);
                frame.setVisible(true);
            }
        }
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
        else if (imageFrame instanceof NavigatorImageDisplayInternalFrame)
            return ((NavigatorImageDisplayInternalFrame) imageFrame).getImageDisplayControl().getImageDisplay();
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

