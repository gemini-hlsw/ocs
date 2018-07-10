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
import edu.gemini.qpt.core.util.Variants.EditException;
import edu.gemini.qpt.ui.util.Platform;
import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.workspace.IShell;

@SuppressWarnings("serial")
public class CompactAction extends AbstractAction implements PropertyChangeListener {

    private static final Logger LOGGER = Logger.getLogger(CompactAction.class.getName());
    private final IShell shell;
    
    public CompactAction(IShell shell) {
        super("Compact Selected Visits");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C, Platform.MENU_ACTION_MASK | KeyEvent.SHIFT_DOWN_MASK));
        shell.addPropertyChangeListener(IShell.PROP_SELECTION, this);
        this.shell = shell;
        setEnabled(false);
    }
    
    @SuppressWarnings("unchecked")
    public synchronized void actionPerformed(ActionEvent e) { // non re-entrant, important
        GSelection<Alloc> sel = (GSelection<Alloc>) shell.getSelection();
        Alloc[] accum = new Alloc[sel.size()];
        long end = sel.first().getStart();
        int i = 0;
        for (Alloc a: sel) {
            try {
                a.forceRemove(); // we're putting it right back, so this is ok >:-/
                accum[i++] = a.getVariant().addAlloc(a.getObs(), end, a.getFirstStep(), a.getLastStep(), a.getSetupType(), a.getComment());
            } catch (EditException pe) {
                LOGGER.log(Level.SEVERE, "Problem moving " + a + " to " + end);
                JOptionPane.showMessageDialog(shell.getPeer(), "The compact action for " + a + " failed due to a " + e.getClass().getSimpleName() + ".\n" +
                        "This should never happen. Sorry.", "Compact Exception", JOptionPane.ERROR_MESSAGE, null);
                return;
            }
            end = accum[i-1].getEnd();
        }
        shell.setSelection(new GSelection<Alloc>(accum));
    }

    public synchronized void propertyChange(PropertyChangeEvent evt) {
        GSelection<?> sel = (GSelection<?>) evt.getNewValue();
        setEnabled(sel.isSelectionOf(Alloc.class) && sel.size() > 1);
    }

}
