package edu.gemini.osgi.main;

import java.io.File;

/**
 * Lookup or calculate the bundle storage root.  Respects the
 * org.osgi.framework.storage property if set, otherwise uses
 *
 *     ~/.ocs15/macAddress/appName/felix-cache
 */
final class AppRoot {

    // BE CAREFUL NOT TO LOG ANYTHING HERE SINCE THAT WILL INITIALIZE LOGGING
    // WITH DEFAULT VALUES

    static final String STORAGE_PROP = "org.osgi.framework.storage";
    private static final String OCS_DIR  = ".ocs15";
    private static final String APP_PROP = "edu.gemini.osgi.main.app";

    public final File dir;
    public final String appName;
    public final File bundleStorage;
    public final boolean explicitlySet;

    AppRoot() throws Exception {
        appName       = getAppName();
        bundleStorage = calcBundleStorageRoot(appName);
        dir           = bundleStorage.getParentFile();
        explicitlySet = System.getProperty(STORAGE_PROP) != null;
    }

    /**
     * Computes the bundle storage root.
     */
    private static File calcBundleStorageRoot(String appName) throws Exception {

        // A previous version of this class included the network mac in the
        // path to accommodate shared home directories mounted from distinct
        // machines.  OCSINF-325 establishes that shared home directories and
        // automated cleanup scripts will not be an issue so it is now a simple
        // ~/.ocs15/ot/felix-cache for everyone, including the telops account.

        final String storage = System.getProperty(STORAGE_PROP);
        if (storage == null) {
            final String[] segments = new String[]{OCS_DIR, appName, "felix-cache"};
            File root = getUserHome();
            for (String s: segments) root = new File(root, s);
            return root;
        } else {
            return new File(storage);
        }
    }

    /**
     * Returns the OCS application name, which must be passed as sys property APP_PROP
     */
    private static String getAppName() {
        final String name = System.getProperty(APP_PROP);
        if (name == null) throw new RuntimeException("OCS app name must be passed via -D" + APP_PROP + "=...");
        return name;
    }

    /**
     * Returns the current user home dir (or user "perm" directory, if it exists).
     */
    private static File getUserHome() {
        final String name = System.getProperty("user.home");
        if (name == null) throw new RuntimeException("user.home is null; this shouldn't happen.");
        final File home = new File(name);
        if (!home.exists()) throw new RuntimeException("user.home (" + name + ") doesn't exist.");
        return home;
    }
}
