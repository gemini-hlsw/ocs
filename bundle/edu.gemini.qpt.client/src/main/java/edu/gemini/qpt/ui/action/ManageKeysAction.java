package edu.gemini.qpt.ui.action;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.ui.util.Platform;
import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.workspace.IShell;
import edu.gemini.util.security.auth.keychain.KeyChain;
import edu.gemini.util.security.ext.auth.ui.AuthDialog;
import edu.gemini.util.security.ext.auth.ui.AuthDialog$;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;

/**
 * Prompts for and executes a SaveAction if needed, then sets the model to null. This action
 * is enabled if the current model is non-null.
 * @author rnorris
 */
public class ManageKeysAction extends AbstractAction {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = Logger.getLogger(ManageKeysAction.class.getName());
	private static final long serialVersionUID = 1L;

	private final IShell shell;
	private final KeyChain authClient;

	public ManageKeysAction(IShell shell, KeyChain authClient) {
		super("Manage Keys...");
		this.shell = shell;
        this.authClient = authClient;
	}

	/**
	 * Save the current schedule if it needs it, then set the model to null.
	 */
	@SuppressWarnings("unchecked")
	public void actionPerformed(ActionEvent e) {
        AuthDialog$.MODULE$.open(authClient, (JComponent) shell.getPeer().getContentPane());
    }

}
