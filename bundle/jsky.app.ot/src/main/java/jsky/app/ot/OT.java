// Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: OT.java 47190 2012-08-02 18:40:21Z swalker $
//
package jsky.app.ot;

import edu.gemini.ags.api.AgsMagnitude;
import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.pot.sp.ISPRootNode;
import edu.gemini.pot.sp.SPNodeKey;
import edu.gemini.pot.spdb.ProgramEvent;
import edu.gemini.pot.spdb.ProgramEventListener;
import edu.gemini.shared.util.immutable.ApplyOp;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.sp.vcs.reg.VcsRegistrar;
import edu.gemini.spModel.core.Peer;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationProviderHolder;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationRepository;
import edu.gemini.spModel.smartgcal.UpdatableCalibrationRepository;
import edu.gemini.spModel.smartgcal.provider.CalibrationProviderImpl;
import edu.gemini.spModel.smartgcal.repository.CalibrationFileCache;
import edu.gemini.spModel.smartgcal.repository.CalibrationRemoteRepository;
import edu.gemini.spModel.smartgcal.repository.CalibrationResourceRepository;
import edu.gemini.spModel.smartgcal.repository.CalibrationUpdater;
import edu.gemini.util.security.auth.keychain.KeyChain;
import jsky.app.ot.gemini.obscat.ObsCatalog;
import jsky.app.ot.modelconfig.Flamingos2Config;
import jsky.app.ot.modelconfig.GemsConfig;
import jsky.app.ot.modelconfig.ModelConfig;
import jsky.app.ot.too.TooPoll;
import jsky.app.ot.tpe.TelescopePosEditor;
import jsky.app.ot.userprefs.observer.ObservingPeer;
import jsky.app.ot.userprefs.observer.ObservingSite;
import jsky.app.ot.util.Resources;
import jsky.app.ot.vcs.vm.VmUpdater;
import jsky.app.ot.viewer.*;
import jsky.app.ot.viewer.action.QueryAction;
import jsky.util.gui.BrowserControl;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.Theme;

import javax.media.jai.JAI;
import javax.media.jai.TileCache;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The main entry point class for OT.  This class initializes
 * various parts, processes any input arguments, and pops up the
 * splash screen if appropriate.
 */
public final class OT {
    private static final Logger LOG = Logger.getLogger(OT.class.getName());

    // Some constants used by the fix to [OT-642] below.
    private static final boolean IS_MAC = System.getProperty("os.name").contains("Mac");
    // --Commented out by Inspection (7/24/13 12:35 PM):public static final boolean IS_WINDOWS = System.getProperty("os.name").contains("Windows");
    public static final boolean IS_MAC_10_5_PLUS = isMac10_5_Plus();
    private static final int MENU_ACTION_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    // Our auth client
    private static KeyChain auth = null;

    // Our magnitude table
    private static AgsMagnitude.MagnitudeTable magTable = null;

    // Determine whether this is OS X, 10.5.x or greater.
    private static boolean isMac10_5_Plus() {
        if (!IS_MAC) return false;

        final String versionStr = System.getProperty("os.version");
        final String[] versionElementsStr = versionStr.split("\\.");
        final int[] versionElements = new int[versionElementsStr.length];
        for (int i = 0; i < versionElementsStr.length; ++i) {
            try {
                versionElements[i] = Integer.parseInt(versionElementsStr[i]);
            } catch (NumberFormatException ex) {
                return false;
            }
        }

        // Handle unexpected versions less or greater than 10.
        if (versionElements[0] < 10) return false;
        if (versionElements[0] > 10) return true;

        // Okay, it's version 10. something.  Should never happen but if there
        // are no more digits, then assume it isn't 10.5 or more.
        if (versionElements.length < 2) return false;

        // If the second version number element is greater than or equal to 5,
        // then we're 10.5 or more (i.e., Leopard or higher)
        return versionElements[1] >= 5;
    }

    // URL for OT help docs.
    private static final String OT_HELP_URL = "http://www.gemini.edu/sciops/OThelp/";

    // Action to use for the "Help" menu item
    private static final AbstractAction _helpAction = new AbstractAction("OT Help") {
        public void actionPerformed(final ActionEvent evt) {
            try {
                help();
            } catch (Exception e) {
                DialogUtil.error(e);
            }
        }
    };

    // Action to use for the "About OT" menu item
    private static final AbstractAction _aboutOTAction = new AbstractAction("About OT") {
        public void actionPerformed(final ActionEvent evt) {
            try {
                SplashDialog.showInstance(false);
            } catch (Exception e) {
                DialogUtil.error(e);
            }
        }
    };


    private static void initSwing() {

        System.setProperty("apple.awt.antialiasing", "on");
        System.setProperty("apple.awt.textantialiasing", "on");

        final ClassLoader cl = OT.class.getClassLoader();
        final Hashtable<Object, Object> tb = UIManager.getDefaults();
        tb.put("ClassLoader", cl);

        // Why are we doing this?

//        for (Enumeration e = tb.keys(); e.hasMoreElements(); ) {
//            Object key = e.nextElement();
//            if (!(key instanceof String)) continue;
//
//            String keyString = (String) key;
//            if (keyString.endsWith("UI")) {
//                try {
//                    Object obj = tb.get(keyString);
//                    if (obj instanceof String) {
//                        Class uic = cl.loadClass((String) obj);
//                        tb.put(uic.getName(), uic);
//                    }
//                } catch (ClassNotFoundException e1) {
//                    throw new RuntimeException(e1);
//                }
//            }
//        }


        Theme.install();

        // [OT-642] Poke around with the keybindings for text fields on the Mac so
        // they respond to Cmd rather than Ctrl. Copied from QPT (Platform.java)
        if (IS_MAC) {
            final JTextComponent[] jtcs = new JTextComponent[]{
                    new JTextArea(),
                    new JTextField(),
                    new JPasswordField(),
            };

            final int CTRL = InputEvent.CTRL_MASK | InputEvent.CTRL_DOWN_MASK;
            for (final JTextComponent jtc : jtcs) {
                for (InputMap map = jtc.getInputMap(); map != null; map = map.getParent()) {
                    if (map.keys() != null) {
                        for (final KeyStroke ks : map.keys()) {
                            final int mod = ks.getModifiers();
                            if ((mod & CTRL) != 0) {
                                final int newMod = (mod & ~CTRL) | MENU_ACTION_MASK;
                                final KeyStroke newKs = KeyStroke.getKeyStroke(ks.getKeyCode(), newMod, ks.isOnKeyRelease());
                                final Object action = map.get(ks);
                                map.remove(ks);
                                map.put(newKs, action);
                            }
                        }
                    }
                }
            }

        }

        UIManager.put("Button.defaultButtonFollowsFocus", true);
    }

    /**
     * Static, Gemini specific initialization: This method (or an equivalent one) should be
     * called once when OT starts.
     */
    private static void initUI() {
        initUI(ObsCatalog.QUERY_MANAGER);
    }


    /**
     * For testing it's often useful to forego the obs catalog stuff. so pass null for qm.
     */
    private static void initUI(final QueryManager qm) {

        initSwing();

        // Add the observatory specific hook for the OT Browser
        if (qm != null) {
            QueryAction.setQueryManager(qm);
        }

        // Register the (partly Gemini specific) position editor features.
        // Note that the order of the buttons in the position editor toolbar
        // will correspond to the order in which the features are registered.
        final String[] features = new String[]{
                "jsky.app.ot.tpe.feat.TpeBasePosFeature",
                "jsky.app.ot.tpe.feat.TpeGuidePosFeature",
                "jsky.app.ot.tpe.feat.TpeTargetPosFeature",
                "jsky.app.ot.tpe.feat.TpeCatalogFeature",
                "jsky.app.ot.gemini.tpe.EdIterOffsetFeature",
                "jsky.app.ot.gemini.acqcam.TpeAcqCameraFeature",
                "jsky.app.ot.gemini.altair.Altair_WFS_Feature",
                "jsky.app.ot.gemini.gems.CanopusFeature",
                "jsky.app.ot.gemini.gsaoi.GsaoiOdgwFeature",
                "jsky.app.ot.gemini.gems.StrehlFeature",
                "jsky.app.ot.gemini.inst.OIWFS_Feature",
                "jsky.app.ot.gemini.tpe.TpePWFSFeature",
                "jsky.app.ot.gemini.inst.SciAreaFeature",
        };

        for (final String featureClass : features) {
            TelescopePosEditor.registerFeature(featureClass);
        }

        // Init the table of editors
        UIInfoXML.init();

    }

    /**
     * Display the OT web page
     */
    private static void help() {
        try {
            BrowserControl.displayURL(OT_HELP_URL);
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }

    public static AbstractAction getHelpAction() {
        return _helpAction;
    }

    public static AbstractAction getAboutOTAction() {
        return _aboutOTAction;
    }

    public interface EditableStateListener {
        /**
         * Gets the node being edited by this component.  This is compared to
         * the source of the update request to determine whether there is a
         * need to call updateEditableState.
         */
        ISPNode getEditedNode();

        /**
         * Performs whatever actions are appropriate when the editable state is
         * potentially updated.
         */
        void updateEditableState();
    }

    private static final List<EditableStateListener> editableStateListeners = new ArrayList<EditableStateListener>();

    /**
     * Fires a property change event that causes the editable states to be updated
     */
    public static void updateEditableState(ISPNode src) {
        if (src == null) return;

        final SPNodeKey root = src.getProgramKey();
        final List<EditableStateListener> copy;
        synchronized (editableStateListeners) {
            copy = new ArrayList<EditableStateListener>(editableStateListeners);
        }

        for (final EditableStateListener esl : copy) {
            final ISPNode n = esl.getEditedNode();
            if ((n != null) && n.getProgramKey().equals(root)) {
                esl.updateEditableState();
            }
        }
    }

    /**
     * Add a listener to be notified when the editable state changes.
     */
    public static void addEditableStateListener(final EditableStateListener l) {
        synchronized (editableStateListeners) {
            editableStateListeners.add(l);
        }
    }

    /**
     * Remove a listener for the editable state changes.
     */
    public static void removeEditableStateListener(final EditableStateListener l) {
        synchronized (editableStateListeners) {
            editableStateListeners.remove(l);
        }
    }


// --Commented out by Inspection START (7/24/13 12:33 PM):
//    //Print a message telling database are incompatible, and then call the updater.
//    private static void _callUpdater(Version otDbVersion, Version remoteDbVersion) {
//        System.out.println("OT DB VERSION     = " + otDbVersion);
//        System.out.println("REMOTE DB VERSION = " + remoteDbVersion);
//        DialogUtil.error("Sorry, your OT version is not compatible with the observing database.");
//        System.exit(0);
//    }
// --Commented out by Inspection STOP (7/24/13 12:33 PM)

    private static void initModelConfig() {
        try {
            final Option<ModelConfig> mc = ModelConfig.load();
            mc.foreach(new ApplyOp<ModelConfig>() {
                @Override
                public void apply(final ModelConfig modelConfig) {
                    GemsConfig.instance.apply(modelConfig);
                    Flamingos2Config.instance.apply(modelConfig);
                }
            });
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Could not load model config.", ex);
        }
    }

    private static void initSmartGcal(final File storageDir) {
        new Thread("Smart GCAL Initializer") {
            @Override
            public void run() {
                if (!storageDir.exists() && !storageDir.mkdir()) {
                    LOG.log(Level.SEVERE, "Could not initialize smart gcal at " + storageDir.getAbsolutePath());
                    return;
                }

                try {
                    // Note: the smartgcal servlet runs in the south only. SmartGcalService must connect to south ODB.
                    final Peer gsPeer = ObservingPeer.peerOrNullFor(Site.GS);
                    if (gsPeer != null) {
                        final CalibrationRepository service = new CalibrationRemoteRepository(gsPeer.host, gsPeer.port);
                        // configure updater as needed
                        final UpdatableCalibrationRepository cachedRepository = new CalibrationFileCache(storageDir);
                        final CalibrationProviderImpl provider = new CalibrationProviderImpl(cachedRepository);
                        CalibrationProviderHolder.setProvider(provider);
                        CalibrationUpdater.instance.addListener(provider);
                        CalibrationUpdater.instance.start(cachedRepository, service, 1l * 60l * 60l * 1000l); // 1x/hour
                        LOG.log(Level.INFO, "smartgcal provider and updater initialized at " + storageDir.getAbsolutePath());
                    }
                } catch (Exception e) {
                    // severe problem: could not initialise smartgcal properly, use a failsafe fallback instead
                    LOG.log(Level.SEVERE, "could not initialise smartgcal, using failsafe fallback instead", e);
                    final CalibrationRepository failsafeRepository = new CalibrationResourceRepository();
                    final CalibrationProviderImpl provider = new CalibrationProviderImpl(failsafeRepository);
                    CalibrationProviderHolder.setProvider(provider);
                }

            }
        }.start();

    }


    private static void initLogging() {
        // Set logging to email problems when using a test version or the OTR.
        LOG.info("Not starting email logger, sorry.");
//        JavaLoggingEmailer.install();
    }

    private static void initNetwork() {
        // this is a global timeout value for network reads (otherwise they just hang)
        final String timeoutStr = String.valueOf(30 * 1000); // convert sec to ms
        System.setProperty("sun.net.client.defaultConnectTimeout", timeoutStr);
        System.setProperty("sun.net.client.defaultReadTimeout", timeoutStr);
    }

    private static void initJAI() {
        new Thread("JAI Initializer") {
            public void run() {
                final TileCache cache = JAI.getDefaultInstance().getTileCache();
                cache.setMemoryCapacity(64 * 1024 * 1024);
            }
        }.start();
    }

    private static void initLocale() {
        // Make sure number and date formatting works the same everywhere
        Locale.setDefault(Locale.US);
    }

    private static void initProgramReplacedSwap() {
        SPDB.get().addProgramEventListener(new ProgramEventListener<ISPProgram>() {

            // Not interested.
            @Override public void programAdded(final ProgramEvent<ISPProgram> pme) { }
            @Override public void programRemoved(final ProgramEvent<ISPProgram> pme) { }

            // If we have an open viewer for this program, replace it.
            @Override public void programReplaced(final ProgramEvent<ISPProgram> pme) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        final ISPProgram newProgram = pme.getNewProgram();
                        final SPNodeKey  key = newProgram.getNodeKey();
                        for (SPViewer v : SPViewer.instances()) {
                            final ISPRootNode root = v.getRoot();
                            if ((root != null) && root.getNodeKey().equals(key)) {
                                v.replaceRoot(newProgram);
                                break;
                            }
                        }
                    }
                });
            }
        });
    }

    private static void initAuth(final KeyChain auth) {
        if (auth == null)
            throw new IllegalArgumentException("KeyChain cannot be null.");
        OT.auth = auth;
    }

    public static KeyChain getKeyChain() {
        return auth;
    }


    private static void initMagnitudeTable(final AgsMagnitude.MagnitudeTable magTable) {
        if (magTable == null) {
            throw new IllegalArgumentException("magTable cannot be null");
        }
        OT.magTable = magTable;
    }

    public static AgsMagnitude.MagnitudeTable getMagnitudeTable() {return magTable; }

    public static Set<Principal> getUser() {
        return getKeyChain().subject().getPrincipals();
    }

    public static void open(final KeyChain auth, final AgsMagnitude.MagnitudeTable magTable, final VcsRegistrar reg, final File storageDir) {

        // Init all the things
        initAuth(auth);
        initMagnitudeTable(magTable);
        initLogging();
        initLocale();
        initJAI();
        initNetwork();
        initUI();
        initModelConfig();
        initSmartGcal(new File(storageDir, "smartgcal"));
        initProgramReplacedSwap();
        VmUpdater.manageUpdates(SPDB.get(), auth, reg);

        // Done. Show the UI
        SplashDialog.init(Resources.getResource("conf/welcome.html"));
        SplashDialog.showInstance(true);

        TooPoll.init(5000);
        ObservingSite.initWhenStaff();
    }

}

