package jsky.app.ot.progadmin;

import edu.gemini.shared.util.TimeValue;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.timeacct.TimeAcctAllocation;
import edu.gemini.spModel.timeacct.TimeAcctAward;
import edu.gemini.spModel.timeacct.TimeAcctCategory;
import jsky.coords.HMS;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class TimeAcctEditor implements ProgramTypeListener {
    private static final long MS_PER_HOUR = Duration.ofHours(1).toMillis();

    // Used to format values as strings
    private final static DecimalFormat nf = new DecimalFormat("0.#");

    private static Duration toDuration(double hours) {
        return Duration.ofMillis(Math.round(hours * MS_PER_HOUR));
    }

    private static final Duration parseDuration(JTextField field) {
        try {
            return toDuration(Double.parseDouble(field.getText()));
        } catch (NumberFormatException ex) {
            return Duration.ZERO;
        }
    }


    private final TimeAcctUI ui;

    private final DocumentListener hoursListener = new DocumentListener() {
        @Override public void insertUpdate(final DocumentEvent event) {
            update();
        }

        @Override public void removeUpdate(final DocumentEvent event) {
            update();
        }

        @Override public void changedUpdate(final DocumentEvent event) {
            update();
        }

        private void update() {
            final TimeAcctAllocation alloc = getAllocation();
            showTotalTime(alloc);
            showPercentTime(alloc);
        }
    };

    public TimeAcctEditor(final TimeAcctUI ui, final ProgramTypeModel pkm) {
        this.ui = ui;

        for (final TimeAcctCategory cat : TimeAcctCategory.values()) {
            ui.getProgramAwardField(cat).getDocument().addDocumentListener(hoursListener);
            ui.getPartnerAwardField(cat).getDocument().addDocumentListener(hoursListener);
        }

        final boolean enable = enableMinimumTime(pkm.getProgramType());
        if (!enable) ui.getMinimumTimeField().setText("");
        ui.getMinimumTimeField().setEnabled(enable);

        pkm.addProgramTypeListener(this);
    }

    public TimeAcctAllocation getAllocation() {

        final Map<TimeAcctCategory, TimeAcctAward> allocMap = new HashMap<>();
        for (final TimeAcctCategory cat : TimeAcctCategory.values()) {
            final Duration  progAward = parseDuration(ui.getProgramAwardField(cat));
            final Duration  partAward = parseDuration(ui.getPartnerAwardField(cat));
            final TimeAcctAward award = new TimeAcctAward(progAward, partAward);
            allocMap.put(cat, award);
        }
        return new TimeAcctAllocation(allocMap);
    }

    private void showTotalTime(final TimeAcctAllocation alloc) {
        final TimeAcctAward a = alloc.getSum();
        ui.getTotalAwardLabel()  .setText(nf.format(a.getTotalHours()));
        ui.getProgramAwardLabel().setText(nf.format(a.getProgramHours()));
        ui.getPartnerAwardLabel().setText(nf.format(a.getPartnerHours()));
    }

    private TimeValue getMinimumTime() {
        final String text = ui.getMinimumTimeField().getText();
        if ((text == null) || "".equals(text.trim())) return TimeValue.ZERO_HOURS;

        double hours = 0;
        try {
            hours = Math.max(Double.parseDouble(text), 0);
        } catch (final NumberFormatException nfex) {
            // If the value can't be parsed as a double, then try the
            // HH:MM:SS format
            try {
                hours = (new HMS(text).getVal());
            } catch (Exception ex) {
                // give up trying to parse the value
            }
        }

        return new TimeValue(hours, TimeValue.Units.hours);
    }

    private void showPercentTime(final TimeAcctAllocation alloc) {
        for (final TimeAcctCategory cat : TimeAcctCategory.values()) {
            final JLabel lab = ui.getPercentLabel(cat);
            final double percent = alloc.getPercentage(cat, TimeAcctAward::getTotalAward);
            lab.setText(String.format("%.0f%%", percent));
        }
    }

    public void setModel(final TimeAcctModel model) {
        TimeValue minTime = model.getMinimumTime();

        if ((minTime == null) || (minTime.getMilliseconds() == 0)) {
            ui.getMinimumTimeField().setText("");
        } else {
            minTime = minTime.convertTo(TimeValue.Units.hours);
            ui.getMinimumTimeField().setText(nf.format(minTime.getTimeAmount())); // REL-434
        }

        final TimeAcctAllocation alloc = model.getAllocation();
        showTotalTime(alloc);

        // Show the time for each category.
        for (final TimeAcctCategory cat : TimeAcctCategory.values()) {
            final double progHours = alloc.getAward(cat).getProgramHours();
            ui.getProgramAwardField(cat).setText(nf.format(progHours)); // REL-434

            final double partHours = alloc.getAward(cat).getPartnerHours();
            ui.getPartnerAwardField(cat).setText(nf.format(partHours)); // REL-434
        }

        // Show the percentage for each category.
        showPercentTime(alloc);
    }

    public TimeAcctModel getModel() {
        return new TimeAcctModel(getAllocation(), getMinimumTime());
    }

    public void programTypeChanged(final ProgramTypeEvent event) {
        final JTextField minTimeField = ui.getMinimumTimeField();

        final boolean enable = enableMinimumTime(event.getNewType());
        final Color bg;
        if (enable) {
            bg = Color.white;
        } else {
            minTimeField.setText("");
            bg = new Color(225, 225, 225);
        }
        minTimeField.setEnabled(enable);
        minTimeField.setBackground(bg);
    }

    private static boolean enableMinimumTime(final ProgramTypeInfo pk) {
        if (pk.getMode() != SPProgram.ProgramMode.QUEUE) return false;
        return pk.getQueueBand() == 3;
    }
}
