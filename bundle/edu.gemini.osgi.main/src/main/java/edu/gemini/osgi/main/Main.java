package edu.gemini.osgi.main;

import java.io.File;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import static edu.gemini.osgi.main.AppRoot.STORAGE_PROP;

public final class Main {
    // PLEASE DON'T ADD A STATIC LOGGER HERE, SINCE IT WILL INITIALIZE
    // LOGGING WITH DEFAULT VALUES.

    public static void main(String[] args) throws Exception {
        final AppRoot root = new AppRoot();
        LogInitializer.initializeLogging(root);
        final Logger log = Logger.getLogger(Main.class.getName());
        checkLocalHostBug(log);
        initializeFrameworkStorage(root, log);
        setNetworkTtl(log);
        log.info("Handing control to Felix");
        org.apache.felix.main.Main.main(args);
    }


    // REL-1966: ISG mail server round-robin bug workaround
    private static final String NETWORK_ADDRESS_CACHE_TTL = "networkaddress.cache.ttl";
    private static void setNetworkTtl(Logger log) throws Exception {
        final Integer ttl = Integer.getInteger(NETWORK_ADDRESS_CACHE_TTL);
        if (ttl != null) {
            java.security.Security.setProperty(NETWORK_ADDRESS_CACHE_TTL, String.valueOf(ttl));
            log.info("Set " + NETWORK_ADDRESS_CACHE_TTL + " to " + ttl + " seconds.");
        }
    }


    // REL-2066: OS X, JDK 1.7 networking issue
    private static final String NETWORKING_BUG_MESSAGE =
        "Your Mac's host name does not include domain information, which exposes a bug\n" +
        "in the version of Java being used by this application.  Adding <tt>.local</tt>\n" +
        "to the host name will fix this issue.  See " +
        "<a href=\"http://www.gemini.edu/sciops/observing-gemini/phase-ii-and-s/w-tools/observing-tool/known-bugs\">OT Known Bugs</a> "+
        "for more information.";

    private static void checkLocalHostBug(Logger log) throws Exception {
        final String osname = System.getProperty("os.name");
        if (osname == null) {
            log.severe("Could not determine the platform on which the app is running.");
            throw new RuntimeException();
        }

        final String losname = osname.toLowerCase();
        if (losname.contains("mac") || losname.contains("os x")) {
            try {
                log.info("Local host is " + java.net.InetAddress.getLocalHost().getHostName());
            } catch (UnknownHostException ex) {
                FatalMessage.show("Mac / Java Networking Issue", NETWORKING_BUG_MESSAGE, log);
            }
        }
    }


    private static final String LOCKFILE = "lockfile";

    private static final String LOCKFILE_TITLE  = "Lockfile Issue";

    private static final String LOCKFILE_EXISTS_MESSAGE  =
        "It appears that you are already running this app on this machine. If you\n" +
        "are <em>sure</em> this is not the case, delete the file below and try again:";

    private static final String LOCKFILE_CREATE_MESSAGE  =
        "Could not create lockfile:";

    private static void lockfileError(String message, File lockfile, Logger log) {
        final String m = String.format("%s<br/><br/>\n\n<tt>%s</tt>", message, lockfile.getAbsoluteFile());
        FatalMessage.show(LOCKFILE_TITLE, m, log);
    }

    private static void initializeFrameworkStorage(AppRoot root, Logger log) throws Exception {
        if (root.explicitlySet) {
            log.info(String.format("Using existing %s: %s", STORAGE_PROP, root.bundleStorage.getAbsolutePath()));
        } else {
            // Make the bundle storage area if necessary
            root.bundleStorage.mkdirs();
            if (!root.bundleStorage.isDirectory())
                throw new RuntimeException("Could not create directory: " + root.bundleStorage);

            // Check lock-file
            final File lock = new File(root.dir, LOCKFILE);
            if (lock.exists()) {
                lockfileError(LOCKFILE_EXISTS_MESSAGE, lock, log);
            } else {
                if (!lock.createNewFile()) lockfileError(LOCKFILE_CREATE_MESSAGE, lock, log);
                lock.deleteOnExit();
            }

            // Set storage
            System.setProperty(STORAGE_PROP, root.bundleStorage.getAbsolutePath());
            log.info(String.format("Set %s to %s", STORAGE_PROP, System.getProperty(STORAGE_PROP)));
        }
    }
}