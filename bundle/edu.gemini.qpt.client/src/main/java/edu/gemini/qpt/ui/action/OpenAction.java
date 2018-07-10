package edu.gemini.qpt.ui.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import edu.gemini.ags.api.AgsMagnitude;
import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.ui.util.ConfigErrorDialog;
import edu.gemini.qpt.ui.util.Platform;
import edu.gemini.qpt.ui.util.UnusedSemesterDialog;
import edu.gemini.ui.workspace.IShell;
import edu.gemini.util.security.auth.keychain.KeyChain;

/**
 * Executes a CloseAction if needed, them prompts for a file and attempts to open it. This
 * action is always enabled.
 * @author rnorris
 */
public class OpenAction extends AbstractOpenAction {

    private static final long serialVersionUID = 1L;

    public OpenAction(IShell shell, KeyChain authClient, AgsMagnitude.MagnitudeTable magTable) {
        super("Open...", shell, authClient, magTable);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, Platform.MENU_ACTION_MASK));
    }

    @Override
    protected void asyncActionPerformed(ActionEvent e) {
        IShell shell = getShell();
        try {
            shell.getPeer().getGlassPane().setVisible(true);
            if (shell.getModel() != null) new CloseAction(shell, authClient).actionPerformed(e);
            if (shell.getModel() == null) {
                Schedule schedule = open();
                if (schedule != null) {
                    shell.setModel(schedule);
                    // Warn the user if there are misconfigured observations.
                    if (shell.getModel() != null) {
                        ConfigErrorDialog.show((Schedule) shell.getModel(), shell.getPeer());
                        UnusedSemesterDialog.show((Schedule) shell.getModel(), shell.getPeer());
                    }
                }
            }
        } finally {
            shell.getPeer().getGlassPane().setVisible(false);
        }
    }

}
