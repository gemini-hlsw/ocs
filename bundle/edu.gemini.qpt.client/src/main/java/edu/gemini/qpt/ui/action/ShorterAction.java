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
public class ShorterAction extends AbstractAction implements PropertyChangeListener {

    private static final Logger LOGGER = Logger.getLogger(ShorterAction.class.getName());

    private final IShell shell;

    private Alloc alloc;
    private Alloc succ;

    public ShorterAction(IShell shell) {
        super("Shorter");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, Platform.MENU_ACTION_MASK));
        this.shell = shell;
        shell.addPropertyChangeListener(this);
        setEnabled(false);
    }

    @SuppressWarnings("unchecked")
    public void actionPerformed(ActionEvent e) {

        Alloc oldAlloc = alloc, oldSucc = succ;
        Variant v = oldAlloc.getVariant();

        try {

            oldAlloc.forceRemove(); // we're putting it right back :-<
            Alloc newAlloc = v.addAlloc(oldAlloc.getObs(), oldAlloc.getStart(), oldAlloc.getFirstStep(), oldAlloc.getLastStep() - 1, oldAlloc.getSetupType(), oldAlloc.getComment());

            if (oldSucc != null) {
                oldSucc.forceRemove(); // BAH

                long offset = 0;
                if (PreferenceManager.get(BooleanToolPreference.TOOL_MAINTAIN_SPACING)) {
                    offset = oldSucc.getObs().getSteps().getStepTime(oldSucc.getFirstStep() - 1);
                }
                v.addAlloc(oldSucc.getObs(), oldSucc.getStart() - offset, oldSucc.getFirstStep() - 1, oldSucc.getLastStep(), oldSucc.getSetupType(), oldSucc.getComment());
            }

            // Now be sure that any slices following newAlloc are far enough away.
            long scheduleTime = newAlloc.getEnd();
            for (Alloc a = newAlloc.getSuccessor(); a != null; a = a.getSuccessor()) {
                if (a.getStart() < scheduleTime) {
                    a = a.move(scheduleTime);
                }
                scheduleTime = a.getEnd();
            }


            shell.setSelection(new GSelection<Alloc>(newAlloc));

        } catch (EditException ee) {
            LOGGER.log(Level.SEVERE, "Problem making " + oldAlloc + " shorter", ee);
            JOptionPane.showMessageDialog(shell.getPeer(), "The 'shorter' action for " + oldAlloc + " failed due to a " + ee.getClass().getSimpleName() + ".\n" +
                    "This should never happen. Sorry.", "Drop Exception", JOptionPane.ERROR_MESSAGE, null);
            return;
        }

    }

    @SuppressWarnings("unchecked")
    public void propertyChange(PropertyChangeEvent evt) {

        // Enabled if and only if there is a single alloc selected.
        if (IShell.PROP_SELECTION.equals(evt.getPropertyName())) {
            GSelection<?> sel = (GSelection<?>) evt.getNewValue();

            if (sel != null && sel.size() == 1 && sel.first() instanceof Alloc) {

                alloc = (Alloc) sel.first();
                succ = alloc.getSuccessor();
                setEnabled(alloc.getLastStep() > alloc.getFirstStep());

            } else {
                alloc = null;
                setEnabled(false);
            }
        }

    }

}
