package edu.gemini.osgi.main;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.logging.Logger;
import javax.swing.*;

import static edu.gemini.osgi.main.AppRoot.STORAGE_PROP;

public final class Main {
    // PLEASE DON'T ADD A STATIC LOGGER HERE, SINCE IT WILL INITIALIZE
    // LOGGING WITH DEFAULT VALUES.

    private static final String NETWORK_ADDRESS_CACHE_TTL = "networkaddress.cache.ttl";

    private static final String LOCKFILE = "lockfile";

    private static final String MESSAGE  =
        "It appears that you are already running this app on this machine. If you\n" +
        "are *sure* this is not the case, delete the file below and try again:\n";

    public static void main(String[] args) throws Exception {
        final AppRoot root = new AppRoot();
        LogInitializer.initializeLogging(root);
        final Logger log = Logger.getLogger(Main.class.getName());
        initializeFrameworkStorage(root, log);
        setNetworkTtl(log);
        log.info("Handing control to Felix");
        org.apache.felix.main.Main.main(args);
    }

    // REL-1966: ISG mail server round-robin bug workaround
    private static void setNetworkTtl(Logger log) throws Exception {
        final Integer ttl = Integer.getInteger(NETWORK_ADDRESS_CACHE_TTL);
        if (ttl != null) {
            java.security.Security.setProperty(NETWORK_ADDRESS_CACHE_TTL, String.valueOf(ttl));
            log.info("Set " + NETWORK_ADDRESS_CACHE_TTL + " to " + ttl + " seconds.");
        }
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
                final String message = MESSAGE + "\n" + lock.getAbsoluteFile();
                System.err.println(message);
                if (!GraphicsEnvironment.isHeadless()) showPopup(message);
                System.exit(-1);
            } else {
                if (!lock.createNewFile()) {
                    final String message = "Cannot create lockfile " + lock.getAbsoluteFile();
                    if (!GraphicsEnvironment.isHeadless()) showPopup(message);
                    throw new RuntimeException(message);
                }
                lock.deleteOnExit();
            }

            // Set storage
            System.setProperty(STORAGE_PROP, root.bundleStorage.getAbsolutePath());
            log.info(String.format("Set %s to %s", STORAGE_PROP, System.getProperty(STORAGE_PROP)));
        }
    }


    // An awful popup so that users who aren't launching the app from the
    // command line at least know what is going on and what to do.
    private static void showPopup(String message) {
        final JPanel pan = new JPanel(new BorderLayout(0, 5)) {{
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        }};
        final JTextArea text = new JTextArea(message, 4, 45) {{
            setEditable(false);
            setOpaque(false);
            setLineWrap(true);
            setWrapStyleWord(true);
        }};
        final JButton ok = new JButton("Ok");

        pan.add(text, BorderLayout.CENTER);
        pan.add(new JPanel() {{ add(ok); }}, BorderLayout.SOUTH);

        final JDialog dialog = new JDialog((Frame)null, "Lock File Issue", true) {{
            setContentPane(pan);
            ok.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            });
        }};
        dialog.pack();

        final Point p = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
        dialog.setLocation(new Point(p.x - dialog.getWidth()/2, p.y - dialog.getHeight()/2));

        dialog.setVisible(true);
    }
}