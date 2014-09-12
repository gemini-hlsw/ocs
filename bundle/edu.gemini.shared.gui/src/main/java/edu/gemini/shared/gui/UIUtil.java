// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: UIUtil.java 11924 2008-08-08 16:03:19Z swalker $
//

package edu.gemini.shared.gui;

import edu.gemini.shared.util.FileUtil;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

/**
 * Collection of user interface utility routines.
 */
public class UIUtil {

    /**
     * A KeyListener that does nothing with the events it receives.
     */
    public static final KeyListener NULL_KEY_LISTENER = new KeyListener() {
        public void keyPressed(KeyEvent me) {
        }

        public void keyReleased(KeyEvent me) {
        }

        public void keyTyped(KeyEvent me) {
        }
    };

    /**
     * A MouseListener that does nothing with the events it receives.
     */
    public static final MouseListener NULL_MOUSE_LISTENER = new MouseListener() {
        public void mouseClicked(MouseEvent me) {
        }

        public void mouseEntered(MouseEvent me) {
        }

        public void mouseExited(MouseEvent me) {
        }

        public void mousePressed(MouseEvent me) {
        }

        public void mouseReleased(MouseEvent me) {
        }
    };

    private static String _baseDir;

    /**
     * Initialize with the base directory of the application (the directory
     * to which the "images" are relative), and the application's frame.
     */
    public static void init(String baseDir) {
        _baseDir = baseDir;
    }

    /**
     * Computes the upper left-hand corner of a square (window) of the given
     * dimensions centered on the screen.  If the square is wider than the
     * screen size, the x coordinate of the returned Point will be 0.  If the
     * square is taller than the screen size, the y coordinate of the returned
     * Point will be 0.
     *
     * @return Point in screen coordinates of where the upper left-hand corner of
     * the window should be displayed
     */
    public static Point getUpperLeftCenteringCoordinate(int width, int height) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = screenSize.width / 2 - width / 2;
        int y = screenSize.height / 2 - height / 2;
        if ((x + width) > screenSize.width) {
            x = 0;
        }
        if ((y + height) > screenSize.height) {
            y = 0;
        }
        return new Point(x, y);
    }

    /**
     * Center the given frame on the screen.
     */
    public static void centerFrame(Frame frame) {
        // These methods aren't available in 1.1 (sadly)
        //int   w = frame.getWidth();
        //int   h = frame.getHeight();
        Dimension d = frame.getSize();
        int w = d.width;
        int h = d.height;
        Point p = getUpperLeftCenteringCoordinate(w, h);
        frame.setLocation(p);
    }

    /**
     * Load the given image file (which is relative to the base directory).
     *
     * @deprecated Use <code>Class.getResource(String name)</code> to get a
     * URL pointing to the image instead.
     */
    @Deprecated
    public static ImageIcon loadImage(String imgFile) {
        String s = File.separator;
        String f = _baseDir + s + "images" + s + imgFile;
        return new ImageIcon(f);
    }

    ///**
    // * Gets the ImageIcon associated with the given name.  The name is specified
    // * as in a call to <code>Class.getResource(String name)</code>.
    // *
    // * @return an ImageIcon which may be used to display the image, or null if
    // * the given image name cannot be found
    // */
    //public static ImageIcon
    //getImage(String name)
    //{
    //   URL imgURL = UIUtil.class.getResource(
    //}

    /**
     * Display an error message in a dialog box.
     */
    public static void error(String message) {
        JOptionPane jp = new JOptionPane(message, JOptionPane.ERROR_MESSAGE);
        JDialog jd = jp.createDialog(null, "Error");
        jd.setVisible(true);
    }

    /**
     * Ask a yes/no question.
     *
     * @return true if yes is selected, no otherwise.
     */
    public static boolean yesNo(String question) {
        return yesNo(question, "Yes/No", null, null);
    }

    /**
     * Ask a yes/no question, supplying the strings to use instead of yes and no.
     *
     * @return true if the yes option is selected, no otherwise.
     */
    public static boolean yesNo(String question, String yesStr, String noStr) {
        return yesNo(question, yesStr + "/" + noStr, yesStr, noStr);
    }

    /**
     * Ask a yes/no question, supplying a title for the dialog box and strings
     * to use instead of "yes" and "no".
     *
     * @return true if yes is selected, no otherwise.
     */
    public static boolean yesNo(String question, String title, String yesStr, String noStr) {
        String[] yesNoOpts = null;
        if ((yesStr != null) && (noStr != null)) {
            yesNoOpts = new String[]{yesStr, noStr};
        }
        int res = JOptionPane.showOptionDialog(null, question, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, yesNoOpts, null);
        return (res == 0);
    }

    /**
     * Select a file for opening.
     *
     * @param initialDir the initial directory to display in the dialog box.
     * If null, this will default to the user's home directory.
     *
     * @param frame the frame that the dialog box should be centered in.  If
     * null, the box will be centered on the screen.
     *
     * @param validate whether to make sure the file can be read
     *
     * @return the selected file if any, otherwise null
     */
    public static File selectFileForOpening(File initialDir, Frame frame, boolean validate) {
        return selectFileForReading(new JFileChooser(), initialDir, frame, validate);
    }

    /**
     * Select a file for reading.  This method differs slightly from the
     * <code>selectFileForOpening</code> method in that it gives the caller
     * control over the initialization of the JFileChooser used.  Of particular
     * interest might be the "approve button" text, tooltip and mnemonic.
     *
     * @param fc the configured JFileChooser to use
     *
     * @param initialDir the initial directory to display in the dialog box.
     * If null, this will default to the user's home directory.
     *
     * @param frame the frame that the dialog box should be centered in; if
     * null, the box will be centered on the screen
     *
     * @param validate whether to make sure the file can be read
     *
     * @return the selected file if any, otherwise null
     */
    public static File selectFileForReading(JFileChooser fc, File initialDir, Frame frame, boolean validate) {
        if (initialDir != null) {
            fc.setCurrentDirectory(initialDir);
        }
        File f = null;
        int res = fc.showDialog(frame, null);
        if (res == JFileChooser.APPROVE_OPTION) {
            f = fc.getSelectedFile();
        }
        if ((f != null) && validate && !isValidReadableFile(f)) {
            return null;
        }
        return f;
    }

    private static File getExistingDir(File dir) {
        if (dir == null) return null;
        if (dir.exists() && dir.isDirectory()) return dir;
        return getExistingDir(dir.getParentFile());
    }

    /**
     * Select a file for reading, using a FileDialog. This provides basically
     * the same as {@link #selectFileForReading(javax.swing.JFileChooser,java.io.File,java.awt.Frame,boolean)}
     * but using a File dialog instead. Useful for showing up in OSX, since
     * a FileDialog in Aqua looks as a native OSX File Dialog
     */
    public static File selectFileForReading(FileDialog fd, File initialDir, boolean validate) {
        if (initialDir != null) {
            initialDir = getExistingDir(initialDir);
            if (initialDir.exists()) fd.setDirectory(initialDir.getPath());
        }
        fd.setMode(FileDialog.LOAD);
        fd.setVisible(true);

        if (fd.getFile() == null) {
            fd.dispose();
            return null; //the user hit cancel
        }

        String fullpath = fd.getDirectory() + fd.getFile();

        fd.dispose();

        File f = new File(fullpath);

        if (validate && !isValidReadableFile(f)) {
            return null;
        }
        return f;
    }

    /**
     * Validate that the given file is a normal readable file, displaying
     * an error box otherwise.
     *
     * @return true if the file is readable
     */
    public static boolean isValidReadableFile(File f) {
        if (!f.exists()) {
            error("'" + f.getAbsolutePath() + "' does not exist.");
            return false;
        }
        if (!(f.canRead() && f.isFile())) {
            error("'" + f.getAbsolutePath() + "' is not readable.");
            return false;
        }
        return true;
    }

    /**
     * Select a file for saving.
     *
     * @param initialSetting the initial directory or file to display in the
     * dialog box.  If argument FILE is a directory, the file chooser will display
     * this directory and a blank file name.  If argument is a file, the chooser
     * will NOT display this file name but the directory will be the file's directory.
     * If null, this will default to the user's home directory.
     * @param frame the frame that the dialog box should be centered in.  If
     * null, the box will be centered on the screen.
     # @param validate whether to make sure the file can be written
     *
     * @return the selected file if any, otherwise null
     */
    public static File selectFileForSaving(File initialSetting, Frame frame, boolean validate) {
        return selectFileForSaving(initialSetting, frame, validate, new JFileChooser());
    }

    /**
     * Select a file for saving.
     *
     * @param initialSetting the initial directory or file to display in the
     * dialog box.  If argument FILE is a directory, the file chooser will display
     * this directory and a blank file name.  If argument is a file, the chooser
     * will NOT display this file's name but the directory will be the file's directory.
     * If null, this will default to the user's home directory.
     * @param frame the frame that the dialog box should be centered in.  If
     * null, the box will be centered on the screen.
     # @param validate whether to make sure the file can be written
     *
     * @return the selected file if any, otherwise null
     */
    public static File selectFileForSaving(File initialSetting, Frame frame, boolean validate, String suffix) {
        return selectFileForSaving(initialSetting, frame, validate, new JFileChooser(), suffix);
    }

    /**
     * Select a file for saving.  This version allows the file chooser to
     * be pre-configured.
     *
     * @param initialSetting the initial directory or file to display in the
     * dialog box.  If argument FILE is a directory, the file chooser will display
     * this directory and a blank file name.  If argument is a file, the chooser
     * will NOT display this file name but the directory will be the file's directory.
     * If null, this will default to the user's home directory.
     * @param frame the frame that the dialog box should be centered in.  If
     * null, the box will be centered on the screen.
     # @param validate whether to make sure the file can be written
     * @param fc the configured JFileChooser to use
     *
     * @return the selected file if any, otherwise null
     */
    public static File selectFileForSaving(File initialSetting, Frame frame, boolean validate, JFileChooser fc) {
        return selectFileForSaving(initialSetting, frame, validate, fc, null);
    }

    /**
     * Select a file for saving.  This version allows the file chooser to
     * be pre-configured.
     * @param initialSetting the initial directory or file to display in the
     * dialog box.  If argument FILE is a directory, the file chooser will display
     * this directory and a blank file name.  If argument is a file, the chooser
     * will NOT display this file name but the directory will be the file's directory.
     * If null, this will default to the user's home directory.
     * @param frame the frame that the dialog box should be centered in.  If
     * null, the box will be centered on the screen.
     # @param validate whether to make sure the file can be written
     * @param fc the configured JFileChooser to use
     * @param suffix if non-null, the saved file will have this suffix
     * added.  But if chosen file has another suffix, that suffix will
     * be used instead.
     *
     * @return the selected file if any, otherwise null
     */
    public static File selectFileForSaving(File initialSetting, Frame frame, boolean validate, JFileChooser fc, String suffix) {
        if (initialSetting != null) {
            fc.setCurrentDirectory(initialSetting);
        }
        File f = null;
        int res = fc.showSaveDialog(frame);
        if (res == JFileChooser.APPROVE_OPTION) {
            f = fc.getSelectedFile();
        }
        if (f == null)
            return null;
        if ((!f.exists()) && suffix != null) {
            // A suffix was requested and the file does not yet exist.
            // Check if it has no suffix, add one of so.
            String selectionSuffix = FileUtil.getSuffix(f);
            if (selectionSuffix == null || selectionSuffix.equals("")) {
                // Add requested suffix
                f = new File(f.getPath() + "." + suffix);
            }
        }
        if (validate) {
            if (f.exists()) {
                // canWrite() seems to work here because we know the file exists
                if (!f.isFile() || !f.canWrite()) {
                    error("Cannot write to '" + f.getAbsolutePath() + "'.");
                    return null;
                }
                if (!yesNo("'" + f.getAbsolutePath() + "' exists. Overwrite?",
                    "Overwrite", "Cancel")) {
                    return null;
                }
            }
        }
        return f;
    }


    /**
     * Select a file for saving, using a FileDialog. This provides basically
     * the same as {@link #selectFileForSaving(java.io.File,java.awt.Frame,boolean,javax.swing.JFileChooser)}
     * but using a File dialog instead. Useful for showing up in OSX, since
     * a FileDialog in Aqua looks as a native OSX File Dialog
     */
    public static File selectFileForSaving(File initialSetting,  boolean validate, FileDialog fd, String suffix) {
        if (initialSetting != null) {
            fd.setDirectory(initialSetting.getPath());
        }
        fd.setMode(FileDialog.SAVE);
        fd.setVisible(true);

        if (fd.getFile() == null) {
            fd.dispose();
            return null; //the user hit cancel
        }

        String fullpath = fd.getDirectory() + fd.getFile();

        fd.dispose();

        File f = new File(fullpath);

        if ((!f.exists()) && suffix != null) {
            // A suffix was requested and the file does not yet exist.
            // Check if it has no suffix, add one of so.
            String selectionSuffix = FileUtil.getSuffix(f);
            if (selectionSuffix == null || selectionSuffix.equals("")) {
                // Add requested suffix
                f = new File(f.getPath() + "." + suffix);
            }
        }
        if (validate) {
            if (f.exists()) {
                // canWrite() seems to work here because we know the file exists
                if (!f.isFile() || !f.canWrite()) {
                    error("Cannot write to '" + f.getAbsolutePath() + "'.");
                    return null;
                }
            }
        }
        return f;
    }


    /**
     * Get the labeled menu item from the given menu.
     */
    public static JMenuItem getMenuItem(JMenu jm, String label) {
        Component[] compA = jm.getMenuComponents();
        JMenuItem jmi = null;
        for (Component aCompA : compA) {
            if (aCompA instanceof JMenuItem) {
                JMenuItem tmp = (JMenuItem) aCompA;
                if (label.equals(tmp.getText())) {
                    jmi = tmp;
                    break;
                }
            }
        }
        return jmi;
    }

    /**
     * Freeze the frame in which the given JComponent resides.  This
     * method uses the "glass pane" of the root pane and so relies upon
     * the getRootPane() method of JComponent.  If the given JComponent
     * is not in a container with a JRootPane, this method will fail.
     *
     * @return true if the frame is frozen
     */
    public static boolean freezeFrame(JComponent jcomp) {
        JRootPane rootPane = jcomp.getRootPane();
        if (rootPane == null) {
            return false;
        }
        Component glass = rootPane.getGlassPane();
        glass.setVisible(true);  // activate the glass pane
        glass.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        glass.requestFocus();

        // Don't know why, but seem to have to add Mouse and Key listeners
        // to intercept input events.  Would have thought that the glass pane's
        // position on top of everything else would have done the trick with
        // no listener's registered ...

        glass.addMouseListener(NULL_MOUSE_LISTENER);
        glass.addKeyListener(NULL_KEY_LISTENER);
        return true;
    }

    /**
     * Thaw the frame in which the given JComponent resides.
     * @see #freezeFrame
     * @return true if the frame is frozen
     */
    public static boolean thawFrame(JComponent jcomp) {
        JRootPane rootPane = jcomp.getRootPane();
        if (rootPane == null) {
            return false;
        }
        Component glass = rootPane.getGlassPane();
        glass.setVisible(false);  // deactivate the glass pane
        return true;
    }

    /**
     * Creates a TitledBorder on an EtchedBorder with a <code>pad</code> pixel
     * empty interior.  This hopefully creates a nicer looking border than having
     * the titled border closely wrapped around its component.
     *
     * @param title the title to use in the border
     * @param pad the interior space to use
     */
    public static TitledBorder createTitledBorder(String title, int pad) {
        Border space = BorderFactory.createEmptyBorder(0, pad, pad, pad);
        Border compound = BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), space);
        return BorderFactory.createTitledBorder(compound, title);
    }

    /**
     * Creates a TitledBorder on an EtchedBorder, a 5 pixel empty interior and
     * the given title.
     */
    public static TitledBorder createTitledBorder(String title) {
        return createTitledBorder(title, 5);
    }

    /**
     * Returns the top level container, usually a JFrame or Window,
     * containing the given <code>{@link Component}</code>.
     * Returns null if component has no parent.
     */
    public static Container getTopLevelContainer(Component component) {
        Container parent = component.getParent();
        Container lastParent = parent;
        while (parent != null) {
            lastParent = parent;
            parent = lastParent.getParent();
        }
        return lastParent;
    }

    /** Convenience routine to avoid working with a <code>Dimension</code>. */
    static public int getPreferredWidth(JComponent c) {
        Dimension d = c.getPreferredSize();
        return d.width;
    }

    /** Convenience routine to avoid working with a <code>Dimension</code>. */
    static public void setPreferredWidth(JComponent c, int iSize) {
        Dimension d = c.getPreferredSize();
        d.width = iSize;
        c.setPreferredSize(d);
    }

    /** Convenience routine to avoid working with a <code>Dimension</code>. */
    static public int getPreferredHeight(JComponent c) {
        Dimension d = c.getPreferredSize();
        return d.height;
    }

    /** Convenience routine to avoid working with a <code>Dimension</code>. */
    static public void setPreferredHeight(JComponent c, int iSize) {
        Dimension d = c.getPreferredSize();
        d.height = iSize;
        c.setPreferredSize(d);
    }

    /** Convenience routine to avoid working with a <code>Dimension</code>. */
    static public void setMaximumWidth(JComponent c, int iSize) {
        Dimension d = c.getMaximumSize();
        d.width = iSize;
        c.setMaximumSize(d);
    }

    /** Convenience routine to avoid working with a <code>Dimension</code>. */
    static public void setMaximumHeight(JComponent c, int iSize) {
        Dimension d = c.getMaximumSize();
        d.height = iSize;
        c.setMaximumSize(d);
    }

    /** Convenience routine to avoid working with a <code>Dimension</code>. */
    static public void setMinimumWidth(JComponent c, int iSize) {
        Dimension d = c.getMinimumSize();
        d.width = iSize;
        c.setMinimumSize(d);
    }

    /** Convenience routine to avoid working with a <code>Dimension</code>. */
    static public void setMinimumHeight(JComponent c, int iSize) {
        Dimension d = c.getMinimumSize();
        d.height = iSize;
        c.setMinimumSize(d);
    }

    /** Convenience routine to avoid working with a <code>Dimension</code>. */
    static public int getWidth(Component c) {
        Dimension d = c.getSize();
        return d.width;
    }

    /** Convenience routine to avoid working with a <code>Dimension</code>. */
    static public void setWidth(Component c, int iSize) {
        Dimension d = c.getSize();
        d.width = iSize;
        c.setSize(d);
    }

    /** Convenience routine to avoid working with a <code>Dimension</code>. */
    static public int getHeight(Component c) {
        Dimension d = c.getSize();
        return d.height;
    }

    /** Convenience routine to avoid working with a <code>Dimension</code>. */
    static public void setHeight(Component c, int iSize) {
        Dimension d = c.getSize();
        d.height = iSize;
        c.setSize(d);
    }

    /**
     * Goes through given JComponent (a Container) and finds the max width
     * of any components and sets them all to this width.
     * Motivation is to make buttons uniform width in a FlowLayout when
     * there is enough free space to do so.
     * Only works if all components are JComponents since it works through
     * setPreferredSize()!
     */
    static public void setContentsUniformWidth(JComponent container) {
        int n = container.getComponentCount();
        int maxW = 0;
        int width;
        for (int i = 0; i < n; ++i) {
            try {
                JComponent comp = (JComponent) container.getComponent(i);
                width = getPreferredWidth(comp);
                if (width > maxW)
                    maxW = width;
            } catch (Exception ex) {
                // Probably found a contained component that was not
                // castable to a JComponent
                return;
            }
        }
        for (int i = 0; i < n; ++i) {
            JComponent comp = (JComponent) container.getComponent(i);
            setPreferredWidth(comp, maxW);
        }
    }

}

