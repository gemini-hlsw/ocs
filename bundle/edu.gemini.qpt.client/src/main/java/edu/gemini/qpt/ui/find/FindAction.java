package edu.gemini.qpt.ui.find;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.shared.sp.Obs;
import edu.gemini.qpt.ui.util.AbstractAsyncAction;
import edu.gemini.qpt.ui.util.Platform;
import edu.gemini.qpt.ui.util.PreferenceManager;
import edu.gemini.qpt.ui.view.candidate.ClientExclusion;
import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.workspace.IShell;
import edu.gemini.util.security.auth.keychain.KeyChain;

/**
 * Executes a CloseAction if needed, then creates a new schedule, prompting the user to
 * choose a site. This action is always enabled.
 * @author rnorris
 */
public class FindAction extends AbstractAsyncAction {

    private static final long serialVersionUID = 1L;
//    private static final Logger LOGGER = Logger.getLogger(FindAction.class.getName());

    private final IShell shell;

    public FindAction(IShell shell, KeyChain authClient) {
        super("Find Candidate...", authClient);
        this.shell = shell;
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F, Platform.MENU_ACTION_MASK));
    }

    @SuppressWarnings("unchecked")
    public void asyncActionPerformed(ActionEvent e) {
        FindElement fe = FindDialog.showFind(shell);

        if (fe != null) {
            Obs obs = (Obs) fe.getTarget();

            if (fe.getError() != null) {
                ClientExclusion ce = (ClientExclusion) fe.getError();

                int ret = JOptionPane.showConfirmDialog(shell.getPeer(),
                        "In order to show this obs, I have to change your view settings. Is that ok?",
                        "Change View Settings", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

                if (ret == JOptionPane.OK_OPTION) {
                    Variant v = ((Schedule) shell.getModel()).getCurrentVariant();
                    while (ce != null) {
                        PreferenceManager.set(ce.getPref(), true);
                        ce = ClientExclusion.forObs(v, obs);
                    }
                } else
                    return; ///
            }

            shell.setSelection(new GSelection<Obs>(obs)); // is this it?
        }

    }

}


