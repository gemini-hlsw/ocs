package edu.gemini.qpt.ui.action;

import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.Date;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JOptionPane;

import edu.gemini.ags.api.AgsMagnitude;
import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.ui.util.SharedIcons;
import edu.gemini.spModel.core.Semester;
import edu.gemini.ui.workspace.IShell;
import edu.gemini.util.security.auth.keychain.KeyChain;

@SuppressWarnings("serial")
public class AddSemesterAction extends RefreshAction {

    public AddSemesterAction(IShell shell, KeyChain authClient, AgsMagnitude.MagnitudeTable magTable) {
        super(shell, authClient, magTable);
        putValue(NAME, "Add Semester...");
        putValue(ACCELERATOR_KEY, null);
    }

    @Override
    protected void asyncActionPerformed(ActionEvent e) {


        Schedule sched = (Schedule) shell.getModel();

        String current = new Semester(sched.getSite(), new Date(sched.getStart())).toString();

        String message =
            "Select an additional semester to add.\n" +
            current + " and valid rollovers are automatic.";

        SortedSet<String> set = new TreeSet<String>(Collections.<Object>reverseOrder());
        set.addAll(sched.getMiniModel().getAllSemesters());
        set.removeAll(sched.getExtraSemesters());
        set.remove(current);

        if (set.isEmpty()) {
            JOptionPane.showMessageDialog(shell.getPeer(), "There are no more semesters available.");
            return;
        }

        Object[] options = set.toArray();

        String extra = (String) JOptionPane.showInputDialog(
            shell.getPeer(), message, "Add Semester", JOptionPane.OK_CANCEL_OPTION,
            SharedIcons.ADD_SEMESTER, options, options[0]);

        if (extra != null) {
            sched.addExtraSemester(extra);
            super.asyncActionPerformed(e);
        }

    }

}
