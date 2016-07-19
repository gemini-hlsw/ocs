/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: SplashFrame.java 4336 2004-01-20 07:57:42Z gillies $
 */

package jsky.app.ot;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;

import edu.gemini.spModel.core.Platform;
import edu.gemini.spModel.core.Version;
import jsky.app.ot.util.Resources;
import jsky.app.ot.viewer.ViewerService;

public final class SplashDialog extends JFrame {

    private static SplashDialog instance;
    private static URL welcomeTextUrl;

    public static void init(URL welcomeTextUrl) {
        SplashDialog.welcomeTextUrl = welcomeTextUrl;
    }

    public static void showInstance(boolean exitVmOnClose) {
        if (instance != null) hideInstance();
        instance = new SplashDialog(welcomeTextUrl, exitVmOnClose);
    }

    public static void hideInstance() {
        if (instance != null) {
            instance.setVisible(false);
            instance.dispose();
            instance = null;
        }
    }

    private SplashDialog(URL welcomeTxtURL, boolean quitOnClose) {
        super("Welcome to Observing Tool  [Version: " + Version.current + "]");
        final SplashPanel splashPanel = new SplashPanel(welcomeTxtURL, quitOnClose);
        getContentPane().add(splashPanel, BorderLayout.CENTER);
        final Dimension dim = splashPanel.getPreferredSize();
        splashPanel.setPreferredSize(dim);
        final Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(screen.width / 2 - dim.width / 2, screen.height / 2 - dim.height / 2);
        int op = quitOnClose ? WindowConstants.EXIT_ON_CLOSE : WindowConstants.DISPOSE_ON_CLOSE;
        setDefaultCloseOperation(op);

        // register and unregister splash dialog if used as "Splash Screen", i.e. quitOnClose = true
        // (splash dialog is also used as "About" box in which case quitOnClose = false)
        if (quitOnClose) {
            // splash dialog is used as splash screen from which plugins etc can be started; don't exit
            // application as long as a dialog that functions as a splash screen is opened (as opposed to about box)
            final JFrame dialog = this;
            ViewerService.instance().get().registerView(dialog);
            addWindowListener(new WindowAdapter() {
                public void windowClosed(WindowEvent e) {
                    ViewerService.instance().get().unregisterView(dialog);
                }
            });
        }

        Resources.setOTFrameIcon(this);

        pack();
        setVisible(true);
        toFront();
    }

}
