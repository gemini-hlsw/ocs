package edu.gemini.qpt.ui.action.nudge;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import edu.gemini.qpt.ui.util.Flash;
import edu.gemini.qpt.ui.util.Platform;
import edu.gemini.ui.workspace.IShell;

@SuppressWarnings("serial")
public class ResolutionHigherAction extends AbstractAction implements PropertyChangeListener {

    private final IShell shell;
    
    public ResolutionHigherAction(IShell shell) {
        super("Slower Nudging");
        this.shell = shell;
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, Platform.MENU_ACTION_MASK));
    }
    
    /**
     * Save the current schedule if it needs it, then set the model to null.
     */
    public synchronized void actionPerformed(ActionEvent e) {
        Resolution.higher();
        Flash.flash(shell.getPeer(), Resolution.caption());
    }

    public void propertyChange(PropertyChangeEvent evt) {        
    }

}



