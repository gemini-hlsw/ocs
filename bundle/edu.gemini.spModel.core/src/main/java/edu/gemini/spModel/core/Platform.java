package edu.gemini.spModel.core;

import java.util.logging.Logger;

/**
 * An enumeration of supported platforms and a method to obtain the platform
 * upon which the client code is running.
 */
public enum Platform {
    linux,
    osx,
    solaris,
    windows;

    private static final Logger LOG = Logger.getLogger(Platform.class.getName());

    private static Platform platform;

    public static Platform get() {
        if (platform != null) return platform;

        String osname = System.getProperty("os.name");
        if (osname == null) {
            LOG.severe("Could not determine what platform the app is running on.");
            throw new RuntimeException();
        }
        osname = osname.toLowerCase();

        if (osname.contains("windows")) {
            platform = windows;
        } else if (osname.contains("mac") || osname.contains("os x")) {
            platform = osx;
        } else if (osname.contains("solaris")) {
            platform = solaris;
        } else {
            platform = linux;
        }
        return platform;
    }
}
