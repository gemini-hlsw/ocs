package edu.gemini.qpt.ui.action;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import edu.gemini.ags.api.AgsMagnitude;
import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.Variant;
import edu.gemini.skycalc.TwilightBoundType;
import edu.gemini.skycalc.TwilightBoundedNight;
import edu.gemini.ui.workspace.IShell;
import edu.gemini.util.security.auth.keychain.KeyChain;

/**
 * Executes a CloseAction if needed, them prompts for a file and attempts to open it. This
 * action is always enabled.
 * @author rnorris
 */
public class MergeAction extends AbstractOpenAction implements PropertyChangeListener {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(MergeAction.class.getName());

    private static final String ERR_FILE = "You can't merge a plan into itself!";
    private static final String ERR_SITE = "The plan you selected is from the other site!";
    private static final String ERR_NIGHT = "The plan you selected seems to be from another night!";

    private static final String PROMPT =
        "This command opens another plan and adds its variants to the current plan.\n" +
        "Is this what you want to do?";

    public MergeAction(IShell shell, KeyChain authClient, AgsMagnitude.MagnitudeTable magTable) {
        super("Merge...", shell, authClient, magTable);
        setEnabled(false);
        shell.addPropertyChangeListener(IShell.PROP_MODEL, this);
    }

    @Override
    protected void asyncActionPerformed(ActionEvent e) {
        IShell shell = getShell();
        try {
            shell.getPeer().getGlassPane().setVisible(true);
            Schedule current = (Schedule) shell.getModel();
            if (current == null) {
                LOGGER.warning("Model is null. Shouldn't be possible.");
                return;
            }

            int opt = JOptionPane.showConfirmDialog(shell.getPeer(), PROMPT, "Merge Plans", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (opt != JOptionPane.YES_OPTION) return;

            Schedule other = open();
            if (other != null) {

                // Can't merge a plan into itself
                if (other.getFile().equals(current.getFile())) {
                    JOptionPane.showMessageDialog(shell.getPeer(), ERR_FILE, "Cannot Merge", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Can't merge plans from different sites
                if (current.getSite() != other.getSite()) {
                    JOptionPane.showMessageDialog(shell.getPeer(), ERR_SITE, "Cannot Merge", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Can't merge plans from different nights (yet)
                TwilightBoundedNight prevNight = new TwilightBoundedNight(TwilightBoundType.NAUTICAL, current.getStart(), current.getSite());
                TwilightBoundedNight nextNight = new TwilightBoundedNight(TwilightBoundType.NAUTICAL, other.getStart(), other.getSite());
                if (prevNight.getStartTime() != nextNight.getStartTime()) {
                    JOptionPane.showMessageDialog(shell.getPeer(), ERR_NIGHT, "Cannot Merge", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // First merge in any extra semesters. Duplicates will be ignored
                for (String semester: other.getExtraSemesters())
                    current.addExtraSemester(semester);

                // And the facilities.
                for (Enum<?> facility: other.getFacilities())
                    current.addFacility(facility);

                // Now just merge the variant sets together
                for (Variant variant: other.getVariants())
                    current.duplicateVariant(variant);

                // The Obs in the new alloc point to the old minimodel, so we will refresh to fix it
                new RefreshAction(shell, authClient, magTable).asyncActionPerformed(e);

            }
        } finally {
            shell.getPeer().getGlassPane().setVisible(false);
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        updateEnabled();
    }

    @Override
    protected void updateEnabled() {
        super.updateEnabled();
        if (isEnabled())
            setEnabled(getShell().getModel() != null);
    }
}
