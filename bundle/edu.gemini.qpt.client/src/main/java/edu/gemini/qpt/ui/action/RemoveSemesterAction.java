package edu.gemini.qpt.ui.action;

import java.awt.event.ActionEvent;
import java.util.Date;
import java.util.SortedSet;

import javax.swing.JOptionPane;

import edu.gemini.ags.api.AgsMagnitude;
import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.ui.util.SharedIcons;
import edu.gemini.spModel.core.Semester;
import edu.gemini.ui.workspace.IShell;
import edu.gemini.util.security.auth.keychain.KeyChain;

@SuppressWarnings("serial")
public class RemoveSemesterAction extends RefreshAction {

    public RemoveSemesterAction(IShell shell, KeyChain authClient, AgsMagnitude.MagnitudeTable magTable) {
        super(shell, authClient, magTable);
        putValue(NAME, "Remove Semester...");
        putValue(ACCELERATOR_KEY, null);
    }

    @Override
    protected void asyncActionPerformed(ActionEvent e) {
        Schedule sched = (Schedule) shell.getModel();
        String current = new Semester(sched.getSite(), new Date(sched.getStart())).toString();
        String message =
            "Select a semester to remove.\n" +
            current + " and valid rollovers cannot be removed.";

        SortedSet<String> set = sched.getExtraSemesters();
        if (set.isEmpty()) {
            JOptionPane.showMessageDialog(shell.getPeer(), "There are no extra semesters to remove.");
            return;
        }

        Object[] options = set.toArray();
        String extra = (String) JOptionPane.showInputDialog(
            shell.getPeer(), message, "Remove Semester", JOptionPane.OK_CANCEL_OPTION,
            SharedIcons.REMOVE_SEMESTER, options, options[0]);

        if (extra != null) {
            try {
                sched.removeExtraSemester(extra);
                super.asyncActionPerformed(e);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(shell.getPeer(), ex.getMessage());
            }
        }

    }

}
