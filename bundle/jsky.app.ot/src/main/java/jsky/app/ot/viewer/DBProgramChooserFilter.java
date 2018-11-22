// Copyright 2003 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: DBProgramChooserFilter.java 8331 2007-12-05 19:16:40Z anunez $
//

package jsky.app.ot.viewer;

import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.core.*;
import edu.gemini.spModel.util.DBProgramInfo;
import jsky.util.Preferences;
import jsky.util.gui.GridBagUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.*;
import java.util.List;

import static java.awt.GridBagConstraints.*;

/**
 * Defines an extra panel to be inserted in the DBProgramChooser window
 * to filter the list of programs displayed there.
 */
public final class DBProgramChooserFilter implements IDBProgramChooserFilter {
    public enum Mode { localOnly, localAndRemote }

    // Used to save settings between sessions
    private static final String PREF_KEY = DBProgramChooserFilter.class.getName();

    private static final SpidMatcher CLASSICAL = new SpidMatcher.TypeMatcher(ProgramType.Classical$.MODULE$);
    private static final SpidMatcher LP        = new SpidMatcher.TypeMatcher(ProgramType.LargeProgram$.MODULE$);
    private static final SpidMatcher FT        = new SpidMatcher.TypeMatcher(ProgramType.FastTurnaround$.MODULE$);
    private static final SpidMatcher QUEUE     = new SpidMatcher.Or(
                                                    new SpidMatcher.TypeMatcher(ProgramType.Queue$.MODULE$),
                                                    new SpidMatcher.TypeMatcher(ProgramType.SystemVerification$.MODULE$),
                                                    new SpidMatcher.TypeMatcher(ProgramType.DirectorsTime$.MODULE$));
    private static final SpidMatcher CAL       = new SpidMatcher.TypeMatcher(ProgramType.Calibration$.MODULE$);
    private static final SpidMatcher LIB       = new SpidMatcher.Or(
                                                   new SpidMatcher.Pattern("^G[NS]-LIB.*"),
                                                   new SpidMatcher.Pattern("^G[NS]-.*-library")
                                                 );
    private static final SpidMatcher ENG       = new SpidMatcher.TypeMatcher(ProgramType.Engineering$.MODULE$);
    private static final SpidMatcher OTHER     = new SpidMatcher.Not(
                                                   new SpidMatcher.Or(CLASSICAL, LP, FT, QUEUE, CAL, LIB, ENG)
                                                 );

    // Construct a check box with a tooltip
    private static JCheckBox mkCheckBox(final String caption, final String tip) {
        return new JCheckBox(caption) {{
            setName(caption.toLowerCase());
            setToolTipText(tip);
        }};
    }

    private final JCheckBox remote      = mkCheckBox("Remote",  "Include programs from the remote site");
    private final JCheckBox classical   = mkCheckBox("C",       "Include classical mode programs");
    private final JCheckBox lp          = mkCheckBox("LP",      "Include large programs");
    private final JCheckBox fastTurn    = mkCheckBox("FT",      "Include fast turnaround programs");
    private final JCheckBox queue       = mkCheckBox("Q/DD/SV", "Include queue, director's time, and system verification programs");
    private final JCheckBox cal         = mkCheckBox("Cal",     "Include calibration programs");
    private final JCheckBox engineering = mkCheckBox("Eng",     "Include engineering programs");
    private final JCheckBox other       = mkCheckBox("Other",   "Include programs in minor observing modes.");
    private final JCheckBox libs        = mkCheckBox("Libs",    "Include library programs");

    private final JComboBox semestersCombo = new JComboBox() {{
        setToolTipText("Show only programs for the selected semester");

        setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                final JLabel lab = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                @SuppressWarnings("unchecked") final Option<Semester> sem = (Option<Semester>) value;
                lab.setText(sem.isEmpty() ? "Any" : sem.getValue().toString());
                return lab;
            }
        });
    }};

    private final JLabel total = new JLabel();
    private final JPanel panel = new JPanel();

    private final Collection<ActionListener> listeners = new ArrayList<ActionListener>();

    public DBProgramChooserFilter(Mode mode) {

        listeners.add(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                storeSemesterSelectionPreference(getSemesterSelection());
            }
        });
        restoreSettings();

        final JLabel totalProgLabel = new JLabel("Total:");
        final JLabel semesterLabel  = new JLabel("Semester:");

        final GridBagUtil layout = new GridBagUtil(panel);
        final int pd;
        if (mode == Mode.localAndRemote) {
            // it only makes sense to show a "remote" filter if we are possibly
            // showing remote programs
            layout.add(remote,       0, 0, 1, 1, 0., 0., NONE, WEST, new Insets(0, 0, 0, 0));
            layout.add(new JPanel(), 1, 0, 1, 1, 1., 0., HORIZONTAL, CENTER, new Insets(0,0,0,0));
            pd = 11;
        } else {
            pd =  0;
        }
        layout.add(classical,       2, 0, 1, 1, 0., 0., NONE, WEST, new Insets(0, pd, 0,  0));
        layout.add(lp,              3, 0, 1, 1, 0., 0., NONE, WEST, new Insets(0,  6, 0,  0));
        layout.add(fastTurn,        4, 0, 1, 1, 0., 0., NONE, WEST, new Insets(0,  6, 0,  0));
        layout.add(queue,           5, 0, 1, 1, 0., 0., NONE, WEST, new Insets(0,  6, 0,  0));
        layout.add(cal,             6, 0, 1, 1, 0., 0., NONE, WEST, new Insets(0,  6, 0,  0));
        layout.add(engineering,     7, 0, 1, 1, 0., 0., NONE, WEST, new Insets(0,  6, 0,  0));
        layout.add(other,           8, 0, 1, 1, 0., 0., NONE, WEST, new Insets(0,  6, 0,  0));
        layout.add(libs,            9, 0, 1, 1, 0., 0., NONE, WEST, new Insets(0,  6, 0,  0));
        layout.add(semesterLabel,  10, 0, 1, 1, 1., 0., NONE, EAST, new Insets(0, 11, 0,  0));
        layout.add(semestersCombo, 11, 0, 1, 1, 0., 0., NONE, WEST, new Insets(0,  6, 0, 11));
        layout.add(totalProgLabel, 12, 0, 1, 1, 1., 0., NONE, EAST, new Insets(0, 11, 0,  0));
        layout.add(total,          13, 0, 1, 1, 0., 0., NONE, WEST, new Insets(0,  6, 0,  0));
    }

    /**
     * Make and return an option panel to be inserted below the list of programs
     */
    public JPanel getFilterPanel() {
        return panel;
    }

    @SuppressWarnings("unchecked")
    private Option<Semester> getSemesterSelection() {
        final Option<Semester> sem = (Option<Semester>) semestersCombo.getSelectedItem();
        return sem == null ? None.<Semester>instance() : sem;
    }

    private static Option<Semester> loadSemesterSelectionPreference() {
        final String key = PREF_KEY + ".semester";
        return ImOption.apply(Preferences.get(key)).flatMap(new MapOp<String, Option<Semester>>() {
            @Override public Option<Semester> apply(String s) {
                try {
                    return new Some<Semester>(Semester.parse(s));
                } catch (ParseException ex) {
                    return None.instance();
                }
            }
        });
    }

    private void storeSemesterSelectionPreference(Option<Semester> sem) {
        final String key = PREF_KEY + ".semester";
        Preferences.set(key, sem.isEmpty() ? null : sem.getValue().toString());
    }

    // Update the contents of the semestersCombo combobox to include all of the semestersCombo
    // in the progIds in the given list of DBProgramInfo objects
    private <A> void updateSemesters(Iterable<A> as, MapOp<A, scala.Option<ProgramId>> getProgramId) {
        final Set<Semester> semesters = new TreeSet<Semester>();
        for (A a : as) {
            final scala.Option<ProgramId> pid = getProgramId.apply(a);
            final scala.Option<Semester> sem  = pid.isEmpty() ? scala.Option.<Semester>empty() : pid.get().semester();
            if (sem.isDefined()) semesters.add(sem.get());
        }

        final List<Option<Semester>> optSemesters = new ArrayList<Option<Semester>>();
        for (Semester s : semesters) optSemesters.add(new Some<Semester>(s));
        optSemesters.add(None.<Semester>instance());
        Collections.reverse(optSemesters);

        @SuppressWarnings("unchecked") final Option<Semester> sel0 = (Option<Semester>) semestersCombo.getSelectedItem();
        final Option<Semester> sel = (sel0 == null) ? loadSemesterSelectionPreference() : sel0;

        _setComboBoxListenersEnabled(false);
        semestersCombo.setModel(new DefaultComboBoxModel(optSemesters.toArray()));
        if (optSemesters.contains(sel)) semestersCombo.setSelectedItem(sel);
        else semestersCombo.setSelectedItem(None.<Semester>instance());
        _setComboBoxListenersEnabled(true);
    }


    // Get the saved settings from the previous session, if any, and arrange to save the user's choices for later use.
    private static void restoreSettings(List<JCheckBox> checks, boolean def) {
        for (final JCheckBox c : checks) {
            final String pref = PREF_KEY + "." + c.getName();
            c.setSelected(Preferences.get(pref, def));
            c.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Preferences.set(pref, Boolean.toString(c.isSelected()));
                }
            });
        }
    }

    private void restoreSettings() {
        restoreSettings(Arrays.asList(remote, classical, lp, fastTurn, queue), true);
        restoreSettings(Arrays.asList(cal, engineering, other, libs),         false);
    }

    // Filters the input list with some glorious UI updating side-effects.
    public <A> List<A> filter(final IDBDatabaseService odb, List<A> as, MapOp<A, scala.Option<ProgramId>> getProgId) {
        updateSemesters(as, getProgId);

        final SpidMatcher matcher = getMatcher(odb);
        final List<A> result = new ArrayList<A>();
        for (A a : as) {
            if (matcher.matches(getProgId.apply(a))) result.add(a);
        }

        total.setText(String.valueOf(result.size()));
        return result;
    }

    /**
     * Return a filtered version of the given list of DBProgramInfo objects
     */
    public List<DBProgramInfo> filter(final IDBDatabaseService odb, List<DBProgramInfo> infoList) {
        return filter(odb, infoList, new MapOp<DBProgramInfo, scala.Option<ProgramId>>() {
            @Override public scala.Option<ProgramId> apply(DBProgramInfo dbProgramInfo) {
                final SPProgramID pid = dbProgramInfo.programID;
                final String pidStr   = (pid == null) ? null : pid.stringValue();
                return (pidStr == null) ? scala.Option.<ProgramId>empty() : new scala.Some<ProgramId>(ProgramId$.MODULE$.parse(pidStr));
            }
        });
    }

    /**
     * Returns the current matcher, based on what's selected in the GUI doodad.
     */
    public SpidMatcher getMatcher(final IDBDatabaseService odb) {
        final SpidMatcher locationMatcher =
            remote.isSelected() ? SpidMatcher.TRUE : new SpidMatcher.LocalMatcher(odb);

        SpidMatcher idMatcher = SpidMatcher.FALSE;
        if (classical.isSelected()) {
            idMatcher = new SpidMatcher.Or(idMatcher, CLASSICAL);
        }
        if (lp.isSelected()) {
            idMatcher = new SpidMatcher.Or(idMatcher, LP);
        }
        if (fastTurn.isSelected()) {
            idMatcher = new SpidMatcher.Or(idMatcher, FT);
        }
        if (queue.isSelected()) {
            idMatcher = new SpidMatcher.Or(idMatcher, QUEUE);
        }
        if (cal.isSelected()) {
            idMatcher = new SpidMatcher.Or(idMatcher, CAL);
        }
        if (engineering.isSelected()) {
            idMatcher = new SpidMatcher.Or(idMatcher, ENG);
        }
        if (other.isSelected()) {
            idMatcher = new SpidMatcher.Or(idMatcher, OTHER);
        }
        if (libs.isSelected()) {
            idMatcher = new SpidMatcher.Or(idMatcher, LIB);
        }

        final SpidMatcher semMatcher =
            new SpidMatcher.SemesterMatcher(getSemesterSelection());

        return new SpidMatcher.And(locationMatcher, idMatcher, semMatcher);
    }

    public void addActionListener(ActionListener l) {
        remote.addActionListener(l);
        classical.addActionListener(l);
        lp.addActionListener(l);
        fastTurn.addActionListener(l);
        queue.addActionListener(l);
        cal.addActionListener(l);
        engineering.addActionListener(l);
        other.addActionListener(l);
        libs.addActionListener(l);
        semestersCombo.addActionListener(l);
        listeners.add(l);
    }

    public void removeActionListener(ActionListener l) {
        remote.removeActionListener(l);
        classical.removeActionListener(l);
        lp.removeActionListener(l);
        fastTurn.removeActionListener(l);
        queue.removeActionListener(l);
        cal.removeActionListener(l);
        engineering.removeActionListener(l);
        other.removeActionListener(l);
        libs.removeActionListener(l);
        semestersCombo.removeActionListener(l);
        listeners.remove(l);
    }

    // Enable or disable the combobox listeners (to avoid recursion when setting defaults)
    private void _setComboBoxListenersEnabled(boolean enabled) {
        for (ActionListener l : listeners) {
            if (enabled) semestersCombo.addActionListener(l);
            else semestersCombo.removeActionListener(l);
        }
    }
}
