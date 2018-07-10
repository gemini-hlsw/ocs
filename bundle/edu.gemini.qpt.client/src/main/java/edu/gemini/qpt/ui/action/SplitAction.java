package edu.gemini.qpt.ui.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.shared.util.TimeUtils;
import edu.gemini.qpt.core.util.Variants.EditException;
import edu.gemini.qpt.ui.util.Platform;
import edu.gemini.spModel.obs.plannedtime.PlannedStepSummary;
import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.workspace.IShell;

@SuppressWarnings("serial")
public class SplitAction extends AbstractAction implements PropertyChangeListener {

    private static final Logger LOGGER = Logger.getLogger(SplitAction.class.getName());
    private static final long OPTIMAL_SIZE = 90 * TimeUtils.MS_PER_MINUTE;

    private final IShell shell;
    private Alloc alloc;

    public SplitAction(IShell shell) {
        super("Split");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, Platform.MENU_ACTION_MASK));
        this.shell = shell;
        shell.addPropertyChangeListener(this);
        setEnabled(false);

    }

    @SuppressWarnings("unchecked")
    public void actionPerformed(ActionEvent e) {

        // Save a reference to the selected alloc, then remove
        // it from the schedule.
        Alloc alloc = this.alloc;
        Variant variant = alloc.getVariant();

        try {

            alloc.forceRemove(); // we're putting it right back...

            // Collect some information about the original alloc's steps.
            int originalFirstStep = alloc.getFirstStep();
            int originalLastStep = alloc.getLastStep();
            int originalStepCount = originalLastStep - originalFirstStep + 1; // inclusive

            // Copy the step times into an array to pass to the partition routine.
            PlannedStepSummary originalPlannedSteps = alloc.getObs().getSteps();
            long originalTotalStepTime = 0;
            long[] originalStepDurations = new long[originalStepCount];
            for (int i = 0; i < originalStepDurations.length; i++) {
                int step = i + originalFirstStep;
                originalStepDurations[i] =
                    originalPlannedSteps.isStepExecuted(step) ? 0 :
                        originalPlannedSteps.getStepTime(step);
                originalTotalStepTime += originalStepDurations[i];
            }

            // We need to partition the step duratinos into n subsequences, where
            // n is at least two, at most originalStepCount, and ideally somewhere
            // close to originalTotalStepTime / OPTIMAL_SIZE.
            int n = (int) Math.min(originalStepCount, originalTotalStepTime / OPTIMAL_SIZE);
            if (n < 2) n = 2;

            // Partition and create the new allocs. Schedule them back to back.
            // Note that each new alloc will include overheads (i.e., acquisition times) so
            // the original alloc will always be less than the sum of its parts by an
            // amount equal to setupTime * (n-1)
            long scheduleTime = alloc.getStart();
            int[] offsets = partition(n, originalStepDurations);
            Alloc[] newSelection = new Alloc[offsets.length];
            for (int i = 0; i < offsets.length; i++) {
                int newFirstStep = originalFirstStep + offsets[i];
                int newLastStep = (i == offsets.length - 1) ? originalLastStep : originalFirstStep + offsets[i + 1] - 1;
                Alloc a = variant.addAlloc(alloc.getObs(), scheduleTime, newFirstStep, newLastStep, alloc.getSetupType(), i == 0 ? alloc.getComment() : null);
                newSelection[i] = a;
                scheduleTime = a.getEnd(); // end time is exclusive, so this doesn't cause an overlap
            }

            // Now be sure that any slices following the last one in newSelection are
            // far enough away.
            for (Alloc a = newSelection[newSelection.length - 1].getSuccessor(); a != null; a = a.getSuccessor()) {
                if (a.getStart() < scheduleTime) {
                    a = a.move(scheduleTime);
                }
                scheduleTime = a.getEnd();
            }

            // Done!
            shell.setSelection(new GSelection<Alloc>(newSelection)); //new Object[] { first } );

        } catch (EditException ee) {
            LOGGER.log(Level.SEVERE, "Problem splitting " + alloc);
            JOptionPane.showMessageDialog(shell.getPeer(), "The split action for " + alloc + " failed due to a " + ee.getClass().getSimpleName() + ".\n" +
                    "This should never happen. Sorry.", "Drop Exception", JOptionPane.ERROR_MESSAGE, null);
            return;
        }

    }

    @SuppressWarnings("unchecked")
    public void propertyChange(PropertyChangeEvent evt) {
        if (IShell.PROP_SELECTION.equals(evt.getPropertyName())) {
            GSelection<?> sel = (GSelection) evt.getNewValue();
            if (sel != null && sel.size() == 1 && sel.first() instanceof Alloc) {
                alloc = (Alloc) sel.first();
                setEnabled(alloc.getFirstStep() < alloc.getLastStep());
            } else {
                alloc = null;
                setEnabled(false);
            }
        }
    }

    /**
     * Partition v[] into n contiguous slices, keeping the values as
     * close to equal as possible. I can't prove that this is correct,
     * but it's fast and seems pretty close.
     */
    private static int[] partition(int n, long... v) {

        LOGGER.fine("Partition " + Arrays.toString(v) + " into " + n + " slices.");

        assert n <= v.length;

        // Going to return n ints.
        int[] ret = new int[n];

        // Determine the ideal size of each partition.
        double ideal = 0;
        for (long l: v) ideal += l;
        ideal /= n;

        // Measure off a slice until it's larger than the ideal size,
        // then either use the slice or the previous one, whichever has
        // the smallest error. Only do the first n-1 slices, the last
        // one gets the remainder, whatever that is.
        int pos = 0;
        for (int i = 0; i < n - 1; i++) {
            ret[i] = pos;
            long total = 0, prev = 0;
            do {
                prev = total;
                total += v[pos++];
            } while (pos < v.length && total < ideal);
            if (Math.abs(prev - ideal) < Math.abs(total - ideal)) {
                total = prev;
                --pos;
            }
        }
        ret[n - 1] = Math.min(pos, v.length - 1);

        // Done.
        return ret;

    }

}
