package edu.gemini.qpt.ui.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.ui.util.Platform;
import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.workspace.IShell;
import edu.gemini.util.security.auth.keychain.KeyChain;

/**
 * Prompts for and executes a SaveAction if needed, then sets the model to null. This action
 * is enabled if the current model is non-null.
 * @author rnorris
 */
public class CloseAction extends AbstractAction implements PropertyChangeListener {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = Logger.getLogger(CloseAction.class.getName());
    private static final long serialVersionUID = 1L;

    private final IShell shell;
    private final KeyChain authClient;

    public CloseAction(IShell shell, KeyChain authClient) {
        super("Close");
        this.shell = shell;
        this.authClient = authClient;
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_W, Platform.MENU_ACTION_MASK));
        shell.addPropertyChangeListener(this);
    }

    /**
     * Save the current schedule if it needs it, then set the model to null.
     */
    @SuppressWarnings("unchecked")
    public void actionPerformed(ActionEvent e) {

        // Save the current schedule if we need to
        Schedule model = (Schedule) shell.getModel();
        if (model != null && model.isDirty()) {

            // HACK: the call to showConfirmDialog will never return if the window
            // isn't focused. This is documented in AWT bug 6179675, which is supposed
            // to be fixed but still happens on some platforms like OS X. note this is
            // only an issue because this action can be called from a non-UI thread
            // as a result of the bundle shutting down.
            shell.getPeer().requestFocus();

            int ret = JOptionPane.showConfirmDialog(shell.getPeer(),
                    "Do you wish to save the current schedule?",
                    "Confirm Close",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            switch (ret) {

            case JOptionPane.YES_OPTION:
                if (model.getFile() == null) {
                    new SaveAsAction(shell, authClient).asyncActionPerformed(e);
                    if (model.getFile() == null)
                        return; // user cancelled save
                } else {
                    new SaveAction(shell, authClient).asyncActionPerformed(e);
                }
                break;

            case JOptionPane.NO_OPTION: break;
            case JOptionPane.CLOSED_OPTION:
            case JOptionPane.CANCEL_OPTION: return;
            default: throw new Error("Impossible.");
            }

        }

        // Done.
        shell.setSelection(GSelection.emptySelection());
        shell.setModel(null);

    }

    public void propertyChange(PropertyChangeEvent evt) {
        setEnabled(shell.getModel() != null);
    }

}
