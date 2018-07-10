package edu.gemini.qpt.ui.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import edu.gemini.qpt.ui.util.Platform;
import edu.gemini.ui.workspace.IShell;

/**
 * Prompts for and executes a SaveAction if needed, then sets the model to null. This action
 * is enabled if the current model is non-null.
 * @author rnorris
 */
public class QuitAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private final IShell shell;
    
    public QuitAction(IShell shell) {
        super("Quit");
        this.shell = shell;
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Q, Platform.MENU_ACTION_MASK));
    }
    
    public void actionPerformed(ActionEvent e) {
        shell.close();
    }
    
}
