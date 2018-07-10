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
import edu.gemini.qpt.ui.util.BooleanToolPreference;
import edu.gemini.qpt.ui.util.Platform;
import edu.gemini.qpt.ui.util.PreferenceManager;
import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.workspace.IShell;

@SuppressWarnings("serial")
public class LongerAction extends AbstractAction implements PropertyChangeListener {

    private static final Logger LOGGER = Logger.getLogger(LongerAction.class.getName());
    
    private final IShell shell;
    
    private Alloc alloc;
    private Alloc succ;
    
    public LongerAction(IShell shell) {
        super("Longer");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, Platform.MENU_ACTION_MASK));
        this.shell = shell;
        shell.addPropertyChangeListener(this);
        setEnabled(false);
    }

    @SuppressWarnings("unchecked")
    public void actionPerformed(ActionEvent e) {
        
        Alloc oldAlloc = alloc, oldSucc = succ;
        try {
            Variant v = oldAlloc.getVariant();
            
            oldAlloc.forceRemove(); // we're putting it right back :-/
            Alloc newAlloc = v.addAlloc(oldAlloc.getObs(), oldAlloc.getStart(), oldAlloc.getFirstStep(), oldAlloc.getLastStep() + 1, oldAlloc.getSetupType(), oldAlloc.getComment());
    
            if (oldSucc != null) {
                oldSucc.forceRemove(); // urgh
                final long offset;
                if (PreferenceManager.get(BooleanToolPreference.TOOL_MAINTAIN_SPACING)) {            
                    offset = oldSucc.getObs().getSteps().getStepTime(oldSucc.getFirstStep());
                } else {
                    offset = Math.max(oldSucc.getStart(), newAlloc.getEnd()) - oldSucc.getStart();
                }
                v.addAlloc(oldSucc.getObs(), oldSucc.getStart() + offset, oldSucc.getFirstStep() + 1, oldSucc.getLastStep(), oldSucc.getSetupType(), oldSucc.getComment());
            }
            
            shell.setSelection(new GSelection<Alloc>(newAlloc)); // new Object[] { newAlloc });

        } catch (EditException ee) {
            LOGGER.log(Level.SEVERE, "Problem making " + oldAlloc + " longer", ee);
            JOptionPane.showMessageDialog(shell.getPeer(), "The 'longer' action for " + oldAlloc + " failed due to a " + ee.getClass().getSimpleName() + ".\n" +
                    "This should never happen. Sorry.", "Drop Exception", JOptionPane.ERROR_MESSAGE, null);
            return;
        }
        
    }

    @SuppressWarnings("unchecked")
    public void propertyChange(PropertyChangeEvent evt) {
        
        // Enabled if and only if there is a single alloc selected.
        if (IShell.PROP_SELECTION.equals(evt.getPropertyName())) {
            GSelection<?> sel = (GSelection) evt.getNewValue();
            if (sel != null && sel.size() == 1 && sel.first() instanceof Alloc) {
                
                alloc = (Alloc) sel.first();
                succ = alloc.getSuccessor();
                if (succ != null) {
                    setEnabled(succ.getLastStep() > succ.getFirstStep());
                } else {
                    setEnabled(alloc.getLastStep() < alloc.getObs().getSteps().size() - 1);
                }
                
            } else {
                alloc = null;
                setEnabled(false);
            }
        }
        
    }

}
