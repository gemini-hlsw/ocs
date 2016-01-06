package jsky.util.gui;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * A simple, static class to display a URL in the system browser.
 * <p>
 * Under Unix, the system browser is hard-coded to be 'netscape'.
 * Netscape must be in your PATH for this to work.  This has been
 * tested with the following platforms: AIX, HP-UX and Solaris.
 * <p>
 * Under Windows, this will bring up the default browser under windows,
 * usually either Netscape or Microsoft IE.  The default browser is
 * determined by the OS.  This has been tested under Windows 95/98/NT.
 * <p>
 * Examples:
 * <p>
 * BrowserControl.displayURL("http://www.javaworld.com")<br>
 * BrowserControl.displayURL("file://c:\\docs\\index.html")<br>
 * BrowserContorl.displayURL("file:///user/joe/index.html");
 * <p>
 * Note - you must include the url type -- either "http://" or
 * "file://".
 * <p>
 * (Based on <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip66.html">
 * Java Tip 66: Control browsers from your Java application</a>
 */
public class BrowserControl {
    enum Platform {
        linux,
        osx,
        solaris,
        windows;

        private static final Logger LOG = Logger.getLogger(Platform.class.getName());

        private static Platform platform;

        static Platform get() {
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

    private static final String WIN_CMD   = "rundll32 url.dll,FileProtocolHandler %s";
    private static final String OSX_CMD   = "open %s";
    private static final String LINUX_CMD = "htmlview %s";
    private static final String LINUX_CMD2 = "xdg-open %s";


    // The default browser under unix.
    private static final String UNIX_PATH = "netscape";

    // The flag to display a url.
    private static final String UNIX_FLAG = "-remote openURL";


    /**
     * Display a file in the system browser.  If you want to display a
     * file, you must include the absolute path name.
     *
     * @param url the file's url (the url must start with either "http://"
     or
     * "file://").
     */
    public static void displayURL(String url) {
        String cmd = "";
        try {
            switch (Platform.get()) {
                case windows:
                    cmd = String.format(WIN_CMD, url);
                    Runtime.getRuntime().exec(cmd);
                    break;
                case osx:
                    cmd = String.format(OSX_CMD, url);
                    Runtime.getRuntime().exec(cmd);
                    break;
                case linux:
                    cmd = String.format(LINUX_CMD, url);
                    try {
                        Runtime.getRuntime().exec(cmd);
                    } catch(Exception e) {
                        // REL-680 'OT Help' not working in Ubuntu
                        cmd = String.format(LINUX_CMD2, url);
                        Runtime.getRuntime().exec(cmd);
                    }
                    break;
                default:
                    // Under Unix, Netscape has to be running for the "-remote"
                    // command to work.  So, we try sending the command and
                    // check for an exit value.  If the exit command is 0,
                    // it worked, otherwise we need to start the browser.
                    // cmd = 'netscape -remote openURL(http://www.javaworld.com)'
                    cmd = UNIX_PATH + " " + UNIX_FLAG + "(" + url + ")";
                    Process p = Runtime.getRuntime().exec(cmd);
                    try {
                        // wait for exit code -- if it's 0, command worked,
                        // otherwise we need to start the browser up.
                        int exitCode = p.waitFor();
                        if (exitCode != 0) {
                            // Command failed, start up the browser
                            // cmd = 'netscape http://www.javaworld.com'
                            cmd = UNIX_PATH + " " + url;
                            Runtime.getRuntime().exec(cmd);
                        }
                    } catch (InterruptedException x) {
                        System.err.println("Error bringing up browser, cmd='" +
                                           cmd + "'");
                        System.err.println("Caught: " + x);
                    }
            }
        } catch (IOException x) {
            // couldn't exec browser
            System.err.println("Could not invoke browser, command=" + cmd);
            System.err.println("Caught: " + x);
        }
    }
}
