// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: AppProperties.java 4336 2004-01-20 07:57:42Z gillies $
//
package jsky.app.ot.util;

import java.io.*;
import java.util.Properties;

/**
 * A class for manipulating application property files.  These are
 * ordinary java.util.Properties files, but they are assumed to be
 * read and written from the user's home directory.  Unless otherwise
 * specified, the property files are stored/read from the ".gemini"
 * directory in the user's home directory.
 */
public class AppProperties {
    /**
     * This is the name of the directory that will be used by default
     * to store the application property files in.  This directory, and
     * any directory explicity specified in one of these method calls
     * is relative to the user's home directory.
     */
    public static final String DEFAULT_DIRECTORY = ".gemini";

    /**
     * Create a java.io.File object pointing to the given directory (relative
     * to the user's home directory) and file.  This method is used
     * internally in this class.
     */
    public static File getAppPropertyFile(String dir, String file) {
        String homedir = System.getProperty("user.home");
        if (homedir == null) {
            return null;
        }

        String absdir = homedir + File.separatorChar + dir;

        return new File(absdir, file);
    }


    /**
     * Create a Properties object and load it from the given file in the
     * user's home directory under .gemini.  If the file doesn't exist
     * or cannot be loaded for some reason, null is returned.
     */
    public static Properties load(String file) {
        return load(DEFAULT_DIRECTORY, file);
    }

    /**
     * Create a Properties object and load it from the given file and
     * directory relative to user's home directory.  If the file doesn't exist
     * or cannot be loaded for some reason, null is returned.
     */
    public static Properties load(String dir, String file) {
        File f = getAppPropertyFile(dir, file);
        if (f == null) {
            return null;
        }

        FileInputStream fis;
        try {
            fis = new FileInputStream(f);
        } catch (FileNotFoundException ex) {
            //System.out.println("Properties file not found: " + f.getAbsolutePath());
            return null;
        }

        Properties p = new Properties();
        try {
            p.load(fis);
        } catch (IOException ex) {
            System.out.println("Error reading: " + f.getAbsolutePath());
            return null;
        }

        return p;
    }

    /**
     * Save the given properties file with the given comment in the given file
     * in the user's home directory under .gemini.  If the file cannot be
     * written to for some reason, false is returned.
     */
    public static boolean save(String file, String comment, Properties p) {
        return save(DEFAULT_DIRECTORY, file, comment, p);
    }

    /**
     * Save the given properties file with the given comment in the given file
     * and directory relative to the user's home directory.  If the file cannot
     * be written to for some reason, false is returned.
     */
    public static boolean save(String dir, String file, String comment, Properties p) {
        File f = getAppPropertyFile(dir, file);
        if (f == null) {
            return false;
        }

        FileOutputStream fos;
        try {
            fos = new FileOutputStream(f);

        } catch (IOException ex) {

            // See if the .gemini directory exists
            String path = f.getParent();
            File fdir = new File(path);
            try {
                if (fdir.exists()) {
                    System.out.println("Error writing: " + f.getAbsolutePath());
                    return false;  // The directory is there so give up
                }

                // Make the .gemini directory.
                fdir.mkdir();

            } catch (Exception ex2) {
                // Trouble checking or creating the directory, give up.
                System.out.println("Error writing: " + f.getAbsolutePath());
                return false;
            }

            // Try again now that the directory exists
            return save(dir, file, comment, p);
        }

        try {
            p.store(fos, comment);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

}
