// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: Resources.java 4726 2004-05-14 16:50:12Z brighton $
//

package edu.gemini.spModel.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resources provides a central class with methods for accessing
 * project resources.
 */
public final class Resources {

    private static final Logger LOGGER = Logger.getLogger(Resources.class.getName());

    /** Path to resources. */
    public static final String RESOURCE_PATH = "/resources";

    /** Subpath to config files, within the resources area. */
    public static final String CONFIG_SUBPATH = "conf";

    /** Path to config files, within the resources area. */
    public static final String CONFIG_PATH = RESOURCE_PATH + "/" + CONFIG_SUBPATH + "/";

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
        if (u == null)
            LOGGER.log(Level.WARNING, "Returning null for missing resource: " + path, new Exception("Stack trace"));
        return u;
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
                }
        }

        return props;
    }
}
