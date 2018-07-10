package edu.gemini.qpt.ui.action;

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
public class DragLimitAction extends AbstractAction implements PropertyChangeListener {

    private final IShell shell;
    private final boolean up;
    
    public DragLimitAction(IShell shell, boolean up) {
        super("Drag Limit " + (up ? "Higher" : "Lower"));
        this.up = up;
        this.shell = shell;
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(up ? KeyEvent.VK_UP : KeyEvent.VK_DOWN, Platform.MENU_ACTION_MASK | KeyEvent.SHIFT_DOWN_MASK));
    }
    
    /**
     * Save the current schedule if it needs it, then set the model to null.
     */
    public synchronized void actionPerformed(ActionEvent e) {
        if (up) DragLimit.higher(); else DragLimit.lower();
        Flash.flash(shell.getPeer(), DragLimit.caption());
    }

    public void propertyChange(PropertyChangeEvent evt) {        
    }

}



