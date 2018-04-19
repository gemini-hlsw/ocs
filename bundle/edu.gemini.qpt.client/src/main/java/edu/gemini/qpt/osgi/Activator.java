package edu.gemini.qpt.osgi;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Hashtable;
import java.util.logging.Logger;

import edu.gemini.ags.conf.ProbeLimitsTable;
import edu.gemini.ictd.IctdDatabase;

import edu.gemini.qpt.core.util.LttsServicesClient;

import edu.gemini.qpt.ui.action.PublishAction;
import edu.gemini.qpt.shared.sp.Ictd;

import edu.gemini.spModel.core.Version;
import edu.gemini.util.security.auth.keychain.KeyChain;
import edu.gemini.util.security.ext.auth.ui.PasswordDialog;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import edu.gemini.qpt.ui.ShellAdvisor;
import edu.gemini.qpt.ui.util.Platform;
import edu.gemini.ui.workspace.IShellAdvisor;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import javax.swing.*;

/**
 * BundleActivator for the QPT application.
 * @author rnorris
 */
public final class Activator implements BundleActivator {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = Logger.getLogger(Activator.class.getName());
    private ShellAdvisor advisor;
    private BundleContext context;

    // Credentials for publishing
    private static final String PROP_USER = "edu.gemini.qpt.ui.action.destination.user";
    private ServiceTracker<KeyChain, KeyChain> keyChainServiceTracker = null;
    private final CtrKeyListener ctrKeyListener = new CtrKeyListener();

    private static String getIctdProperty(final String name, final BundleContext ctx) {
        final String fullName = "edu.gemini.ictd." + name;
        final String value    = ctx.getProperty(fullName);

        if (value == null) {
            final String message = "Missing bundle.properties value " + fullName;
            LOGGER.severe(message);
            throw new RuntimeException(message);
        }

        return value;
    }

    private static Ictd.SiteConfig getIctdSiteConfig(final BundleContext ctx) {
        final String user     = getIctdProperty("user", ctx);
        final String password = getIctdProperty("password", ctx);

        return new Ictd.SiteConfig(
            new IctdDatabase.Configuration(getIctdProperty("gn", ctx), user, password),
            new IctdDatabase.Configuration(getIctdProperty("gs", ctx), user, password)
        );
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    public void start(final BundleContext context) throws Exception {

        LttsServicesClient.LTTS_SERVICES_NORTH_URL = context.getProperty("edu.gemini.qpt.ltts.services.north.url");
        if (LttsServicesClient.LTTS_SERVICES_NORTH_URL == null) {
            LOGGER.warning("Missing bundle.properties value edu.gemini.qpt.ltts.services.north.url");
        }
        LttsServicesClient.LTTS_SERVICES_SOUTH_URL = context.getProperty("edu.gemini.qpt.ltts.services.south.url");
        if (LttsServicesClient.LTTS_SERVICES_SOUTH_URL == null) {
            LOGGER.warning("Missing bundle.properties value edu.gemini.qpt.ltts.services.south.url");
        }

        this.context = context;

        // TODO: this is set to the application install dir where we can find
        // the help files
        final String root = new File(System.getProperty("user.dir")).toURI().toString();

        keyChainServiceTracker = new ServiceTracker<>(context, KeyChain.class, new ServiceTrackerCustomizer<KeyChain, KeyChain>() {
            private ServiceRegistration<?> shellRegistration;

            @Override
            public KeyChain addingService(ServiceReference<KeyChain> acRef) {
                final KeyChain ac = context.getService(acRef);

                final PublishAction.Destination internal, pachon;

                internal = new PublishAction.Destination(
                    "gnconfig.gemini.edu",
                    getProp(PROP_USER),
                    "/gemsoft/var/data/qpt",
                    "http://internal.gemini.edu/science/");

                pachon = new PublishAction.Destination(
                    "gsconfig.gemini.edu",
                    getProp(PROP_USER),
                    "/gemsoft/var/data/qpt",
                    null);

                // If the keychain is locked, give the user the chance to unlock it here. If they
                // choose not to, they can do it via Edit > Manage Keys
                SwingUtilities.invokeLater(() -> {
                    if (ac.asJava().isLocked())
                        PasswordDialog.unlock(ac, null);
                });

                Activator.this.advisor = new ShellAdvisor("Gemini QPT", Version.current.toString(), root, ac, internal, pachon, getIctdSiteConfig(context), ProbeLimitsTable.loadOrThrow());
                shellRegistration = context.registerService(IShellAdvisor.class.getName(), advisor, new Hashtable<>());
                return ac;
            }

            @Override
            public void modifiedService(ServiceReference<KeyChain> serviceReference, KeyChain keyChain) {
                // nop
            }

            @Override
            public void removedService(ServiceReference<KeyChain> serviceReference, KeyChain keyChain) {
                shellRegistration.unregister();
            }
        });
        keyChainServiceTracker.open();

        if (Boolean.getBoolean("ctrl.key.hack")) {
            LOGGER.info("Enabling ctrl key hack.");
            Toolkit.getDefaultToolkit().addAWTEventListener(ctrKeyListener, AWTEvent.KEY_EVENT_MASK);
        }

    }

    private String getProp(String key) {
        String val = context.getProperty(key);
        if (val == null) {
            throw new RuntimeException("Configuration key " + key + " was not specified.");
        }
        return val;
    }

    public void stop(BundleContext context) throws Exception {
        Toolkit.getDefaultToolkit().removeAWTEventListener(ctrKeyListener);
        this.keyChainServiceTracker.close();
        this.keyChainServiceTracker = null;
        this.advisor = null;
        this.context = null;
    }

    private final class CtrKeyListener implements AWTEventListener {

        public void eventDispatched(final AWTEvent event) {
            final KeyEvent ke = (KeyEvent) event;
            switch (ke.getKeyCode()) {
                case KeyEvent.VK_RIGHT:
                case KeyEvent.VK_LEFT:
                    if ((ke.getModifiers() & Platform.MENU_ACTION_MASK) == 0) {
                        ke.consume();
                        final KeyEvent ke2 = new KeyEvent((Component) ke.getSource(), ke.getID(), ke.getWhen(),
                                ke.getModifiers() | Platform.MENU_ACTION_MASK, ke.getKeyCode(), ke.getKeyChar());
                        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(ke2);
                    }
            }
        }
    }

}
