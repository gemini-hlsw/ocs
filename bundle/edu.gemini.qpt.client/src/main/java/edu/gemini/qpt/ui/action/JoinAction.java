package edu.gemini.qpt.ui.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.core.util.Variants.EditException;
import edu.gemini.qpt.ui.util.Platform;
import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.workspace.IShell;

@SuppressWarnings("serial")
public class JoinAction extends AbstractAction implements PropertyChangeListener {

    private static final Logger LOGGER = Logger.getLogger(JoinAction.class.getName());
    
    @SuppressWarnings("unused")
    private final IShell shell;
    
    private Alloc alloc;
    
    public JoinAction(IShell shell) {
        super("Join");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SLASH, Platform.MENU_ACTION_MASK));
        this.shell = shell;
        shell.addPropertyChangeListener(this);
        setEnabled(false);

    }

    @SuppressWarnings("unchecked")
    public void actionPerformed(ActionEvent e) {

        // Ok, what we need to do is take the selected alloc and join it to any
        // abutting allocs in either direction, out to the end. This way split and
        // join are inverse operations.
        
        // Find the first one in the set.
        Alloc first = alloc;
        for (;;) {
            Alloc pred = first.getPredecessor();
            if (!first.abuts(pred)) break;
            first = pred;
        }
        
        Alloc last = first;
        try {

            // Now walk forward to find the last one, removing them
            // as we go.
            for (;;) {
                Alloc succ = last.getSuccessor(); // must do this before removing
                last.forceRemove(); // *ACK* .. ugly. should go in reverse. must do this after finding succ
                if (!last.abuts(succ)) break;
                last = succ;
            }
            
            // Now create a new alloc that sits where first was, extending to
            // the last step in last. Easy.
            Variant v = first.getVariant();
            Alloc newAlloc = v.addAlloc(first.getObs(), first.getStart(), first.getFirstStep(), last.getLastStep(), first.getSetupType(), first.getComment());
            
            // Done!
            shell.setSelection(new GSelection<Alloc>(newAlloc)); // new Object[] { newAlloc } );
            
        } catch (EditException ee) {
            LOGGER.log(Level.SEVERE, "Problem joining " + first + " with " + last);
            JOptionPane.showMessageDialog(shell.getPeer(), "The join action for " + first + "... failed due to a " + ee.getClass().getSimpleName() + ".\n" +
                    "This should never happen. Sorry.", "Drop Exception", JOptionPane.ERROR_MESSAGE, null);
            return;
        }
    }

    @SuppressWarnings("unchecked")
    public void propertyChange(PropertyChangeEvent evt) {
        
        // Enabled if and only if there is a single alloc selected, and it abuts a predecessor or
        // successor.
        if (IShell.PROP_SELECTION.equals(evt.getPropertyName())) {
            GSelection<?> sel = (GSelection) evt.getNewValue();
            if (sel != null && sel.size() == 1 && sel.first() instanceof Alloc) {
                alloc = (Alloc) sel.first();
                setEnabled(alloc.abuts(alloc.getPredecessor()) || alloc.abuts(alloc.getSuccessor()));
            } else {
                alloc = null;
                setEnabled(false);
            }
        }
        
    }

}
