package edu.gemini.qpt.ui.util;

import java.awt.Frame;
import java.util.Collection;
import java.util.HashSet;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.Variant;

@SuppressWarnings("serial")
public class UnusedSemesterDialog extends JDialog {

    private static final String TITLE = "Unused Semester Warning";

    public static void show(Schedule sched, Frame parent) {
        Collection<String> extraSemesters = new HashSet<String>(sched.getExtraSemesters());
        for (Variant v: sched.getVariants()) {
            for (Alloc a: v.getAllocs()) {
                if (extraSemesters.isEmpty()) return;
                a.getObs().getProg().getSemesterAsJava().foreach(s -> extraSemesters.remove(s.toString()));
            }
        }
        show(extraSemesters, parent);
    }

    public static void show(Collection<String> extraSemesters, Frame parent) {
        int size = extraSemesters.size();
        if (size > 0) {

            String FORMAT =
                size == 1 ?
                "You might want to remove semester %s from the plan; it is not being used." :
                "You might want to remove semesters %s from the plan; they are not being used.";

            JOptionPane.showMessageDialog(parent, String.format(FORMAT, extraSemesters), TITLE, JOptionPane.WARNING_MESSAGE);
        }
    }

}
