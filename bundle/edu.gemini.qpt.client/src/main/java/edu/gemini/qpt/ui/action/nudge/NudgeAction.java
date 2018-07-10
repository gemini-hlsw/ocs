package edu.gemini.qpt.ui.action.nudge;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.workspace.IShell;

public abstract class NudgeAction extends AbstractAction implements PropertyChangeListener {

    private final IShell shell;
    
    protected NudgeAction(String name, IShell shell) {
        super(name);
        shell.addPropertyChangeListener(IShell.PROP_SELECTION, this);
        this.shell = shell;
        setEnabled(false);
    }
    
    @SuppressWarnings("unchecked")
    public synchronized void actionPerformed(ActionEvent e) { // non re-entrant, important
        Alloc[] all = (Alloc[]) shell.getSelection().toArray(Alloc.class); 
        for (int i = 0; i < all.length; i++) {        
            Alloc a = all[i];
            long start = a.constrainStartTime(a.getStart() + getNudgeDelta(), a.getLength());
            all[i] = a.move(start);        
        }
        shell.setSelection(new GSelection<Alloc>(all));
    }

    public synchronized void propertyChange(PropertyChangeEvent evt) {
        GSelection<?> sel = (GSelection<?>) evt.getNewValue();
        setEnabled(sel.isSelectionOf(Alloc.class));
    }

    /** Should return nudge offset in positive or negative milliseconds */
    protected abstract long getNudgeDelta();

}
