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
public class ResolutionLowerAction extends AbstractAction implements PropertyChangeListener {

    private final IShell shell;
    
    public ResolutionLowerAction(IShell shell) {
        super("Faster Nudging");
        this.shell = shell;
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_UP, Platform.MENU_ACTION_MASK));
    }
    
    public synchronized void actionPerformed(ActionEvent e) {
        Resolution.lower();
        Flash.flash(shell.getPeer(), Resolution.caption());
    }

    public void propertyChange(PropertyChangeEvent evt) {        
    }

}
