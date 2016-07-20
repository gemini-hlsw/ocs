// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: Resources.java 24803 2010-03-28 03:13:18Z swalker $
//

package jsky.app.ot.util;

import java.awt.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import javax.swing.ImageIcon;

import edu.gemini.shared.util.LruCache;
import edu.gemini.spModel.core.Platform;


/**
 * Resources provides a central class with methods for accessing
 * project resources.
 */
public final class Resources {
    /** Path to resources. */
    public static final String RESOURCE_PATH = "/resources";

    /** Subpath to images, within the resources area. */
    public static final String IMAGES_SUBPATH = "images";

    /** Path to images, within the resources area. */
    public static final String IMAGES_PATH = RESOURCE_PATH + "/" + IMAGES_SUBPATH + "/";

    /** Subpath to config files, within the resources area. */
    public static final String CONFIG_SUBPATH = "conf";

    /** Path to config files, within the resources area. */
    public static final String CONFIG_PATH = RESOURCE_PATH + "/" + CONFIG_SUBPATH + "/";

    /** Subpath to I18N property files, within the resources area. */
    public static final String I18N_SUBPATH = "i18n";

    /** Path to I18N property files, within the resources area. */
    public static final String I18N_PATH = "resources/ot/" + I18N_SUBPATH + "/";

    // ImageCache provides a cache of already present icons.
    private static LruCache<String, ImageIcon> _rmap = new LruCache<String, ImageIcon>(100);

    // Disallow instances
    private Resources() {
    }

    /**
     * Gets a URL associated with the given resource.
     *
     * @return a URL pointing to the file, null otherwise
     */
    public static URL getResource(String fileName) {
        String path = RESOURCE_PATH + '/' + fileName;
        //System.out.println("getResource: " + path);
        URL u = Resources.class.getResource(path);
        if (u == null) System.out.println("Failed to get: " + path);
        return u;
    }


    /**
     * Returns an Icon from the specified filename.
     *
     * @param iconFileName The relative path name to the image file.
     * For example, "flag.gif".  It is assumed the image file is
     * being properly installed to the resources directory.
     *
     * @return Icon constructed from data in <code>iconFileName</code>.
     * Even though the method interface can't guarantee this, the icon
     * implementation will be <code>Serializable</code>.
     * Returns null (does not throw an Exception) if specified
     * resource is not found.
     */
    public static ImageIcon getIcon(String iconFileName) {
        // First check the map under the iconFileName
        ImageIcon icon = _rmap.get(iconFileName);
        if (icon != null) {
            //System.out.println("Found icon: " + iconFileName);
            return icon;
        }

        // Turn the file name into a resource path
        URL url = getResource(IMAGES_SUBPATH + "/" + iconFileName);
        if (url == null) return null;

        icon = new ImageIcon(url);
        _rmap.put(iconFileName, icon);
        return icon;
    }

    public static void setOTFrameIcon(java.awt.Frame frame) {
        if (Platform.get() != Platform.osx) {
            // This has been reported to crash the JVM on OSX when the application
            // is bundled as a DMG. Starting from the DMG The application fails to start
            // without any visible indication on why it has failed
            // However, we want to do this on Windows and Linux
            ImageIcon icon = Resources.getIcon("ot.png");
            if (icon != null) {
                Image image = icon.getImage();
                frame.setIconImage(image);
            }
        }
    }

    /**
     * Loads an installed <code>Properties</code> file.
     *
     * @param fileName relative path to the configuration file (which must be
     * loadable by the <code>java.util.Properties</code> class)
     *
     * @return a Properties object created from the configuration file; null if
     * the file does not exist
     */
    public static Properties getProperties(String fileName)
            throws IOException {
        URL url = getResource(CONFIG_SUBPATH + "/" + fileName);
        if (url == null) {
            return null;
        }

        Properties props = null;
        BufferedInputStream bis = null;

        try {
            bis = new BufferedInputStream(url.openStream());
            props = new Properties();
            props.load(bis);
        } finally {
            if (bis != null)
                try {
                    bis.close();
                } catch (Exception ex) {
                    // ignore
                }
        }

        return props;
    }
}
